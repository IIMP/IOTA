package org.iota.jota.dto.request;

import org.iota.jota.IotaAPICommand;

/**
 * This class represents the core API request 'getTransactionsToApprove'.
 **/
public class IotaGetTransactionsToApproveRequestcompass extends IotaCommandRequest {

    private Integer depth;
    private String reference;

    /**
     * Initializes a new instance of the IotaGetTransactionsToApproveRequestcompass class.
     * 
     * @param depth
     * @param reference
     */
    private IotaGetTransactionsToApproveRequestcompass(final Integer depth, final String reference) {
        super(IotaAPICommand.GET_TRANSACTIONS_TO_APPROVEcompass);
        this.depth = depth;
        this.reference = reference;
    }

    /**
     * Initializes a new instance of the IotaGetTransactionsToApproveRequestcompass class.
     * 
     * @param depth
     * @param reference
     * @return the instance
     */
    public static IotaGetTransactionsToApproveRequestcompass createIotaGetTransactionsToApproveRequestcompass(Integer depth, final String reference) {
        return new IotaGetTransactionsToApproveRequestcompass(depth, reference);
    }

    /**
     * Gets the depth.
     *
     * @return The depth.
     */
    public Integer getDepth() {
        return depth;
    }

    /**
     * Sets the depth.
     *
     * @param depth The depth.
     */
    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public String getReference() {
        return reference;
    }

    /**
     * Sets a reference transaction for tip selection
     *
     * @param reference
     */
    public void setReference(String reference) {
        this.reference = reference;
    }
}
