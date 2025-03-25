import java.sql.SQLException;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.inv.InvSerial;
import org.guanzon.cas.inv.services.InvControllers;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testInvSerial {
    static GRiderCAS instance;
    static InvSerial record;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        instance = MiscUtil.Connect();
        
        try {
            record = new InvControllers(instance, null).InventorySerial();
        } catch (SQLException | GuanzonException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNewRecord() {
        try {
            JSONObject loJSON;

            loJSON = record.newRecord();
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }    

            loJSON = record.getModel().setStockId("M00124000119");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     

            loJSON = record.getModel().setBranchCode(instance.getBranchCode());
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            } 

            loJSON = record.getModel().setClientId("");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            } 

            loJSON = record.getModel().setSerial01("001");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }  

            loJSON = record.getModel().setSerial02("002");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }  

            loJSON = record.getModel().setStockId("M00125000001");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }  
            
            loJSON = record.SerialRegistration().setFileNumber("123456789");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }  

            loJSON = record.saveRecord();
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }  
        } catch (SQLException | GuanzonException e) {
            Assert.fail(e.getMessage());
        }
    }
    
//   
//    @Test
//    public void testUpdateRecord() {
//        JSONObject loJSON;
//
//        loJSON = record.openRecord("M00124000119");
//        if ("error".equals((String) loJSON.get("result"))) {
//            Assert.fail((String) loJSON.get("message"));
//        }      
//        
//        loJSON = record.updateRecord();
//        if ("error".equals((String) loJSON.get("result"))) {
//            Assert.fail((String) loJSON.get("message"));
//        }      
//        
//        loJSON = record.getModel().setDescription("Sample item from new program structure updated.");
//        if ("error".equals((String) loJSON.get("result"))) {
//            Assert.fail((String) loJSON.get("message"));
//        }     
//        
//        loJSON = record.getModel().setModifyingId(instance.getUserID());
//        if ("error".equals((String) loJSON.get("result"))) {
//            Assert.fail((String) loJSON.get("message"));
//        }     
//        
//        loJSON = record.getModel().setModifiedDate(instance.getServerDate());
//        if ("error".equals((String) loJSON.get("result"))) {
//            Assert.fail((String) loJSON.get("message"));
//        }     
//        
//        loJSON = record.saveRecord();
//        if ("error".equals((String) loJSON.get("result"))) {
//            Assert.fail((String) loJSON.get("message"));
//        } 
//    }
    
//    @Test
//    public void testSearch(){
//        JSONObject loJSON = record.searchRecord("", false);        
//        if ("success".equals((String) loJSON.get("result"))){
//            System.out.println(record.getModel().getRegionId());
//            System.out.println(record.getModel().getRegioneName());
//        } else System.out.println("No record was selected.");
//    }
    
    @AfterClass
    public static void tearDownClass() {
        record = null;
        instance = null;
    }
}
