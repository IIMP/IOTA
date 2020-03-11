package com.iota.iri.service.dto;

import com.iota.iri.model.Hash;
import com.iota.iri.service.API;

/**
 * 
 * Contains information about the result of a successful {@code getTransactionsToApprove} API call.
 * See {@link API#getTransactionsToApproveStatement} for how this response is created.
 *
 */
public class GetTransactionsToApproveResponsecompass extends AbstractResponse {

    /**
     * The trunk transaction tip to reference in your transaction or bundle
     */
    private String trunkTransaction;
    // private String trunkSummary;
    /**
     * The branch transaction tip to reference in your transaction or bundle
     */
    private String branchTransaction;
    // private String branchSummary;
    private String summary;

    /**
     * Creates a new {@link GetTransactionsToApproveResponsecompass}
     * 
     * @param trunkTransactionToApprove {@link #trunkTransaction}
     * @param branchTransactionToApprove {@link #branchTransaction}
     * @return a {@link GetTransactionsToApproveResponsecompass} filled with the provided tips
     */
	public static AbstractResponse create(Hash trunkTransactionToApprove, Hash branchTransactionToApprove, String summaryHash) {
		GetTransactionsToApproveResponsecompass res = new GetTransactionsToApproveResponsecompass();
		res.trunkTransaction = trunkTransactionToApprove.toString();
        res.branchTransaction = branchTransactionToApprove.toString();
        // res.trunkSummary = trunkSummary;
        // res.branchSummary = branchSummary;
        res.summary = summaryHash;
        return res;
        
	}
	
    /**
     * 
     * @return {@link #branchTransaction}
     */
	public String getBranchTransaction() {
		return branchTransaction;
	}
	
    /**
     * 
     * @return {@link #trunkTransaction}
     */
	public String getTrunkTransaction() {
		return trunkTransaction;
    }
    // public String getTrunkSummary(){
    //     return trunkSummary;
    // }
    // public String getBranchSummary(){
    //     return branchSummary;
    // }
    public String getSummary(){
        return summary;
    }
}
