package org.iota.jota.dto.response;

/**
 * Response of {@link jota.dto.request.IotaGetTransactionsToApproveRequest}.
 **/
public class GetTransactionsToApproveResponsecompass extends AbstractResponse {

    private String trunkTransaction;
    private String branchTransaction;
    // private String trunckSummary;
    // private String branchSummary;
    private String summary;
    /**
     * Gets the trunk transaction.
     *
     * @return The trunk transaction.
     */
    public String getTrunkTransaction() {
        return trunkTransaction;
    }
    /**
     * Gets the trunk Summary.
     *
     * @return The trunk Summary.
     */
    // public String getTrunckSummary() {
    //     return trunckSummary;
    // }

    /**
     * Gets the branch transaction.
     *
     * @return The branch transaction.
     */
    public String getBranchTransaction() {
        return branchTransaction;
    }
    /**
     * Gets the branch Summary.
     *
     * @return The branch Summary.
     */
    // public String getBranchSummary() {
    //     return branchSummary;
    // }
    public String getSummary(){
        return summary;
    }
}