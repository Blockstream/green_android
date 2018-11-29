package com.greenaddress.greenapi.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import org.bitcoinj.core.Coin;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CreateTransactionResponseData extends JSONData {
    // {"available_total":70626226,"change_address":"2N2SoCJsTJv1d4QDbNWJXw64U6mzERx1LmY","change_amount":9888694,"change_index":0,
    // "change_subaccount":0,"error":"","fee":206,"fee_rate":1000,"have_change":true,"satoshi":10000000,"send_all":false,
    // "transaction":"02000000000101b315c98ee9ca32dce75a0ff8351f0ac60966ca6ccc1bf6bb9566fb06c749c72c0100000023220020d3e54eb3891fa0ec88d1b4c4bc9db22995e17f9c5fcce54ccfada3e474d8b4e3fdffffff02b6e396000000000017a91464e96d7eaef357ce6bba54b3e31baad540eb95a787fcb101000000000017a914b1679f4b4ec35deef6344b9418398c64f46f220b870147304402202e1ea92283b5df0f020c9a6a4db3aa56989b73d8885ce197f9e726b2b6a2e8660220102f04efe8f2c3355d3f353b2e6e79ed097901fd9f0b75e3e899b236c2160dce0148921500",
    // "transaction_size":225,"transaction_vsize":169,"transaction_wieght":675,"used_utxos":[0],
    // "utxos":[{"block_height":1411998,"ga_asset_id":1,"pointer":128,"pt_idx":1,"script_type":14,"subaccount":0,"subtype":0,
    // "txhash":"2cc749c706fb6695bbf61bcc6cca6609c60a1f35f80f5ae7dc32cae98ec915b3","value":10000000},{"block_height":1412195,
    // "ga_asset_id":1,"pointer":138,"pt_idx":0,"script_type":14,"subaccount":0,"subtype":0,
    // "txhash":"76214c0ec8274590a8e8816792ddcb076858effbc0dcee1db001ab8aac5faf02","value":60626226}]}

    private Integer changeIndex;
    private Boolean haveChange;
    private String transaction;
    private String error;
    private Long availableTotal;
    private Long fee;
    private Long value;
    private Long changeAmount;
    private String changeAddress;
    private Integer changeSubaccount;
    private Boolean sendAll;

    @JsonIgnore
    public Coin getFeeAsCoin() {
        return fee == null ? null : Coin.valueOf(fee);
    }

    @JsonIgnore
    public Coin getAvailableTotalAsCoin() {
        return availableTotal == null ? null : Coin.valueOf(availableTotal);
    }

    @JsonIgnore
    public Coin getValueAsCoin() {
        return value == null ? null : Coin.valueOf(value);
    }


    public Long getValue() {
        return value;
    }

    public void setValue(final Long value) {
        this.value = value;
    }

    public String getError() {
        return error;
    }

    public void setError(final String error) {
        this.error = error;
    }

    public Integer getChangeIndex() {
        return changeIndex;
    }

    public void setChangeIndex(final Integer changeIndex) {
        this.changeIndex = changeIndex;
    }

    public Boolean getHaveChange() {
        return haveChange;
    }

    public void setHaveChange(final Boolean haveChange) {
        this.haveChange = haveChange;
    }

    public String getTransaction() {
        return transaction;
    }

    public void setTransaction(final String transaction) {
        this.transaction = transaction;
    }

    public Long getAvailableTotal() {
        return availableTotal;
    }

    public void setAvailableTotal(final Long availableTotal) {
        this.availableTotal = availableTotal;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(final Long fee) {
        this.fee = fee;
    }

    public Long getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(final Long changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getChangeAddress() {
        return changeAddress;
    }

    public void setChangeAddress(final String changeAddress) {
        this.changeAddress = changeAddress;
    }

    public Integer getChangeSubaccount() {
        return changeSubaccount;
    }

    public void setChangeSubaccount(final Integer changeSubaccount) {
        this.changeSubaccount = changeSubaccount;
    }

    public Boolean getSendAll() {
        return sendAll;
    }

    public void setSendAll(final Boolean sendAll) {
        this.sendAll = sendAll;
    }
}
