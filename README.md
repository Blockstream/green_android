# Blockstream Green - A native bitcoin wallet for Android

[![Build Status](https://travis-ci.org/Blockstream/green_android.png?branch=master)](https://travis-ci.org/Blockstream/green_android)

## What is Blockstream Green?

Blockstream Green is a non-custodial Bitcoin wallet - it allows you to safely store, send, and receive your Bitcoin. 

It's a mobile app available for Android and [iOS](https://github.com/Blockstream/green_ios), based on [gdk](https://github.com/blockstream/gdk), our cross-platform wallet library.

We offer a variety of advanced features, such as letting our users set their own spending limits, watch-only access for observers, and our unique multisig security model.
All of these (and more) are explained in more detail [here](https://docs.blockstream.com/green/getting-started/intro.html).

<a href="https://f-droid.org/packages/com.greenaddress.greenbits_android_wallet/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="50"/></a>
<a href="https://play.google.com/store/apps/details?id=com.greenaddress.greenbits_android_wallet" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="50"/></a>

## Build

For instructions on how to build Blockstream Green please refer to [BUILD.md](BUILD.md)

## Contributing

Guidelines for contributions can be found in [CONTRIBUTING.md](CONTRIBUTING.md)

## Translations

You can help translating this app [here](https://www.transifex.com/blockstream/blockstream-green/)

## Support

Need help? 

Read [our FAQ](https://greenaddress.it/en/faq.html) or contact us at [info@greenaddress.it](mailto:info@greenaddress.it).  

## License

Blockstream Green is released under the terms of the GNU General Public License. See [LICENSE](LICENSE) for more information or see https://opensource.org/licenses/GPL-3.0 

## Authenticity

Verifying the APK signing certificate fingerprint is very important for you own security - please follow this steps to make sure the APK you've downloaded is authentic.

Unzip the APK and extract the file ```/META-INF/GREENADD_.RSA```; then run:

```
keytool -printcert -file GREENADD.RSA
```

You will get the certificate fingerprints; verify it matches with:

```
Certificate fingerprints:
	 MD5:  60:D0:C6:E1:B7:8B:5F:E7:E1:94:B6:B8:7D:54:D0:73
	 SHA1: 7F:05:E3:DC:29:CB:E6:76:F5:0A:56:A2:80:1A:FD:37:91:96:8F:7A
	 SHA256: 32:F9:CC:00:B1:3F:BE:AC:E5:1E:2F:B5:1D:F4:82:04:4E:42:AD:34:A9:BD:91:2F:17:9F:ED:B1:6A:42:97:0E
	 Signature algorithm name: SHA256withRSA
	 Version: 3
```

Now download the list of cryptographic checksums: ```SHA256SUMS.asc```

Verify that the checksum of the release file is listed in the checksums file using the following command:

``` 
shasum -a 256 --check SHA256SUMS.asc
```

In the output produced by the above command, you must ensure the output lists "OK" after the name of the release file you downloaded. 

Now import our GPG key:

``` 
04BE BF2E 35A2 AF2F FDF1  FA5D E7F0 54AA 2E76 E792
```

Verify that the checksums file is PGP signed by our key:

```
gpg --verify SHA256SUMS.asc
```

Check the output from the above command for the following text:

A line that starts with: 
```gpg: Good signature```

A complete line saying:  ```Primary key fingerprint: 04BE BF2E 35A2 AF2F FDF1  FA5D E7F0 54AA 2E76 E792```