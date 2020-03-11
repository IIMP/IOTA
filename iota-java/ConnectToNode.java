import org.iota.jota.IotaAPI;
import org.iota.jota.dto.response.GetNodeInfoResponse;
public class ConnectToNode {
    
    public static void main(String[] args) {
    
            // Create a new instance of the API object
            // and specify which node to connect to
            IotaAPI api = new IotaAPI.Builder()
                .protocol("http")
                .host("127.0.0.1")
                .port(14265)
                .build();
    
            // Call the `getNodeInfo()` method for information about the node and the Tangle
            GetNodeInfoResponse response = api.getNodeInfo();
            System.out.println(response);
        }
    }