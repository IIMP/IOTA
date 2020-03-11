package com.iota.iri.service.dto;

/**
 * Response of {@link jota.dto.request.IotaGetStorageRequest}.
 **/
public class GetStorageResponse extends AbstractResponse {

    private byte[] storage;

    /**
     * Gets the Storage.
     *
     * @return The Storage.
     */
    public byte[] getStorage() {
        return storage;
    }
    public static AbstractResponse create(byte[] val) {
		GetStorageResponse res = new GetStorageResponse();
		res.storage = val;
		return res;
	}
	
}
