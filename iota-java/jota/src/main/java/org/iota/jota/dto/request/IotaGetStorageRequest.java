package org.iota.jota.dto.request;

import org.iota.jota.IotaAPICommand;

/**
 * This class represents the core api request 'getBalances'.
 **/
public class  IotaGetStorageRequest extends IotaCommandRequest {

    private String addr;
    private Integer key;

    /**
     * Initializes a new instance of the IotaGetStorageRequest class.
     * 
     * @param threshold
     * @param addr
     * @param tips
     */
    private IotaGetStorageRequest(final String addr, final Integer key) {
        super(IotaAPICommand.GET_STORAGE);
        this.addr = addr.toLowerCase();
        this.key = key;
    }

    /**
     * Initializes a new instance of the IotaGetStorageRequest class.
     * 
     * @param threshold
     * @param addr
     * @param tips
     * @return the instance
     */
    public static IotaGetStorageRequest createIotaGetStorageRequest(final String addr, final Integer key) {
        return new IotaGetStorageRequest(addr, key);
    }

    /**
     * Gets the addr.
     *
     * @return The addr.
     */
    public String getAddr() {
        return addr;
    }

    /**
     * Sets the addr.
     *
     * @param addr The addr.
     */
    public void setAddr(String addr) {
        this.addr = addr;
    }

    /**
     * Gets the threshold.
     *
     * @return The threshold.
     */
    public Integer getKey() {
        return key;
    }

    /**
     * Sets the threshold.
     *
     * @param threshold The threshold.
     */
    public void setKey(Integer key) {
        this.key = key;
    }


}

