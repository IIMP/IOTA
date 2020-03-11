package org.iota.jota.dto.response;

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
}
