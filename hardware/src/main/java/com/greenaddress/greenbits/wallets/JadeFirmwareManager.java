package com.greenaddress.greenbits.wallets;

import android.util.Base64;
import android.util.Log;

import com.blockstream.hardware.R;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.greenaddress.greenapi.HWWalletBridge;
import com.greenaddress.jade.HttpRequestProvider;
import com.greenaddress.jade.JadeAPI;
import com.greenaddress.jade.entities.VersionInfo;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


// Class to handle jade firmware validation and OTA updates (with fw server)
// Separated from main wallet functions in JadeHWWallet
public class JadeFirmwareManager {
    private static final String TAG = "JadeFirmwareManager";

    private static final String JADE_MIN_ALLOWED_FW_VERSION = "0.1.23";
    private static final String JADE_FW_VERSIONS_FILE = "LATEST";

    private static final String JADE_FW_SERVER_HTTPS = "https://jadefw.blockstream.com";
    private static final String JADE_FW_SERVER_ONION = "http://vgza7wu4h7osixmrx6e4op5r72okqpagr3w6oupgsvmim4cz3wzdgrad.onion";

    private static final String JADE_FW_JADE_PATH = "/bin/jade/";
    private static final String JADE_FW_JADEDEV_PATH = "/bin/jadedev/";
    private static final String JADE_FW_SUFFIX = "fw.bin";

    private static final String JADE_BOARD_TYPE_JADE = "JADE";
    private static final String JADE_FEATURE_SECURE_BOOT = "SB";

    private HWWalletBridge parent;
    private final HttpRequestProvider httpRequestProvider;

    // A firmware instance on the file server
    private static class FwFileData {
        final String filepath;
        final String version;
        final String config;
        final int fwSize;
        private byte[] firmware;

        FwFileData(final String filepath, final String version, final String config, final int fwSize) {
            this.filepath = filepath;
            this.version = version;
            this.config = config;
            this.fwSize = fwSize;
            this.firmware = null;
        }

        public byte[] getFirmware() {
            return firmware;
        }

        public void setFirmware(final byte[] firmware) {
            this.firmware = firmware;
        }
    }

    public JadeFirmwareManager(final HWWalletBridge parent, HttpRequestProvider httpRequestProvider) {
        this.parent = parent;
        this.httpRequestProvider = httpRequestProvider;
    }

    // Check Jade fw against minimum allowed firmware version
    private static boolean isJadeFwValid(final String version) {
        return JADE_MIN_ALLOWED_FW_VERSION.compareTo(version) <= 0;
    }

    // Check Jade version info to deduce which firmware flavour/directory to use
    private static String getFirmwarePath(final VersionInfo info) {
        if (info.getBoardType() == null || JADE_BOARD_TYPE_JADE.equals(info.getBoardType())) {
            // Alas the first version of the jade fw didn't have 'BoardType' - so we assume an early jade.
            if (info.getJadeFeatures().contains(JADE_FEATURE_SECURE_BOOT)) {
                // Production Jade (Secure-Boot [and flash-encryption] enabled)
                Log.d(TAG, "Production Jade detected");
                return JADE_FW_JADE_PATH;
            } else {
                // Unsigned/development/testing Jade
                Log.d(TAG, "dev/test Jade detected");
                return JADE_FW_JADEDEV_PATH;
            }
        } else {
            Log.w(TAG, "Unsupported hardware detected - " + info.getBoardType());
            return null;
        }
    }

    // Uses GDKSession's httpRequest() to get file from Jade firmware server - ensures Tor use as appropriate.
    private byte[] downloadJadeFwFile(final String fwFilePath, final boolean isBase64) throws IOException {
        final URL tls = new URL(JADE_FW_SERVER_HTTPS + fwFilePath);
        final URL onion = new URL(JADE_FW_SERVER_ONION + fwFilePath);
        final String certificate = CharStreams.toString(new InputStreamReader(
                this.parent.getResources().openRawResource(R.raw.jade_services_certificate),
                Charsets.UTF_8));

        // Make http GET call to fetch file
        Log.i(TAG, "Fetching firmware file: " + fwFilePath);
        final JsonNode ret = httpRequestProvider.getHttpRequest().httpRequest("GET",
                Arrays.asList(tls, onion),
                null,
                isBase64 ? "base64" : "text",
                Collections.singletonList(certificate));

        if (ret == null || !ret.has("body")) {
            throw new IOException("Failed to fetch firmware file: " + fwFilePath);
        }

        final String body = ret.get("body").asText();
        return isBase64 ? Base64.decode(body, Base64.DEFAULT) : body.getBytes();
    }

    // Get index file (eg. LATEST) as appropriate for the passed info from the fw server
    // Parse into a list of entries of firmwares we could fetch/ota
    private List<FwFileData> getAvailableFirmwares(final VersionInfo verInfo) {
        try {
            // Get relevant fw path (or if hw not supported)
            final String fwPath = getFirmwarePath(verInfo);
            if (fwPath == null) {
                Log.w(TAG, "Unsupported hardware, firmware updates not available");
                return Collections.emptyList();
            }

            // Get the index file from that path
            final String versionIndexFilePath = fwPath + JADE_FW_VERSIONS_FILE;
            final byte[] versions = downloadJadeFwFile(versionIndexFilePath, false);

            // Parse the filenames referenced
            // (Always filter on filename suffix, just to be sure)
            final String[] fwFilenames = new String(versions).split("\n");
            final List<FwFileData> fwFiles = new ArrayList<>(fwFilenames.length);
            for (final String filename : fwFilenames) {
                final String[] parts = filename.split("_");
                if (parts.length == 4 && JADE_FW_SUFFIX.equals(parts[3])) {
                    fwFiles.add(new FwFileData(fwPath + filename, parts[0], parts[1], Integer.parseInt(parts[2])));
                } else {
                    Log.w(TAG, "Ignoring unexpected firmware filename: " + filename);
                }
            }
            return fwFiles;
        } catch (final Exception e) {
            Log.w(TAG, "Error downloading firmware index file: " + e);
            return Collections.emptyList();
        }
    }

    // Load firmware file into the data object
    private void loadFirmware(final FwFileData fwFile) {
        try {
            // Load file from fw server
            final byte[] fw = downloadJadeFwFile(fwFile.filepath, true);
            fwFile.setFirmware(fw);
        } catch (final Exception e) {
            Log.w(TAG, "Error downloading firmware file: " + e);
        }
    }

    private void doOtaUpdate(final JadeAPI jade, final int chunksize, final FwFileData fwFile) throws IOException {
        try {
            // Call jade ota update
            // NOTE: the return value is not that useful, as the OTA may have look like it has succeeded
            // but when jade boots it may decide that new partition is not good, and boot the prior one.
            Log.i(TAG, "Uploading firmware, compressed size: " + fwFile.getFirmware().length);
            final boolean updated = jade.otaUpdate(fwFile.getFirmware(), fwFile.fwSize, chunksize, null);
            Log.i(TAG, "Jade OTA Update returned: " + updated);
            jade.disconnect();

            // Sleep to allow jade to reboot
            android.os.SystemClock.sleep(5000);
        } catch (final Exception e) {
            Log.e(TAG, "Error during firmware update: " + e);
            jade.disconnect();
            android.os.SystemClock.sleep(1000);
        }

        // Regardless of OTA success, fail, error etc. we try to reconnect.
        if (!jade.connect()) {
            throw new IOException("Failed to reconnect to Jade after OTA");
        }
    }

    // Checks version info and attempts to OTA if required.
    // Exceptions are not usually allowed to propagate out of the function, as failure to access the
    // fw-server, to read the index or download the firmware are not errors that prevent the connection
    // to Jade being made.
    // The function returns whether the current firmware is valid/allowed, regardless of any OTA occurring.
    Single<Boolean> checkFirmware(final JadeAPI jade, final boolean deviceHasPinFilter) {
        return Single.create(emitter -> {
            try {
                // Do firmware check and ota if necessary
                final VersionInfo verInfo = jade.getVersionInfo();
                final String currentVersion = verInfo.getJadeVersion();
                final boolean fwValid = isJadeFwValid(currentVersion);
                if (verInfo.getHasPin() != deviceHasPinFilter) {
                    emitter.onSuccess(fwValid);
                    return;
                }

                // Log if current firmware not valid wrt the allowed minimum version
                if (!fwValid) {
                    Log.w(TAG, "Jade firmware is not sufficient to satisfy minimum supported version.");
                    Log.w(TAG, "Allowed minimum: " + JADE_MIN_ALLOWED_FW_VERSION);
                    Log.w(TAG, "Current version: " + currentVersion);
                }

                // Fetch any available/selected firmware update
                final List<FwFileData> availableFirmwares = getAvailableFirmwares(verInfo);

                // Filter to later versions only
                // FIXME: temporary filter to restrict to same config
                final List<FwFileData> updates = new ArrayList<>(availableFirmwares.size());
                for (final FwFileData fw : availableFirmwares) {
                    if (fw.version.compareTo(currentVersion) > 0
                            && verInfo.getJadeConfig().equalsIgnoreCase(fw.config)) {
                        updates.add(fw);
                    }
                }
                if (updates.isEmpty()) {
                    Log.i(TAG, "No firmware updates currently available.");
                    emitter.onSuccess(fwValid);
                    return;
                }

                // Get first/only match then offer as Y/N to user
                // FIXME: show user full list and let them choose
                final FwFileData fwFile = updates.get(0);

                parent.jadeAskForFirmwareUpgrade(fwFile.version, !fwValid, isPositive -> {
                    if(isPositive){
                        // Update firmware
                        final Disposable unused = Single.just(fwFile)
                                .subscribeOn(Schedulers.computation())
                                .doOnSuccess(fw -> Log.i(TAG, "Loading selected firmware file: " + fwFile.filepath))
                                .subscribe(fw -> {
                                            loadFirmware(fwFile);
                                            if (fw.getFirmware() == null) {
                                                emitter.onSuccess(fwValid);
                                            } else {
                                                try {
                                                    // Try to OTA the fw onto Jade
                                                    doOtaUpdate(jade, verInfo.getJadeOtaMaxChunk(), fwFile);

                                                    // Check fw validity again (from scratch)
                                                    final VersionInfo newInfo = jade.getVersionInfo();
                                                    final boolean fwNowValid = isJadeFwValid(newInfo.getJadeVersion());
                                                    emitter.onSuccess(fwNowValid);
                                                } catch (final Exception e) {
                                                    emitter.onError(e);
                                                }
                                            }
                                        },
                                        emitter::onError);
                    }else{
                        // User declined to update firmware right now
                        final Disposable unused = Single.just(fwFile)
                                .subscribeOn(Schedulers.computation())
                                .doOnSuccess(fw -> Log.i(TAG, "No OTA firmware selected"))
                                .subscribe(fw -> emitter.onSuccess(fwValid),
                                        emitter::onError);
                    }
                    return null;
                });
            } catch (final Exception e) {
                emitter.onError(e);
            }
        });
    }
}
