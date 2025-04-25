
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.inv.Inventory;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testInventory {
    static GRiderCAS instance;
    static Inventory record;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        instance = MiscUtil.Connect();
        
        record = new Inventory();
        record.setApplicationDriver(instance);
        record.setWithParentClass(false);
        record.initialize();
    }

    @Test
    public void testNewRecord() {
        try {
            JSONObject loJSON;

            loJSON = record.newRecord();
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }    

            loJSON = record.getModel().setBarCode("000-000-000-024");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     

            loJSON = record.getModel().setDescription("TMX 125 Alpha Standard 2025");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     
            
            loJSON = record.getModel().setBriefDescription("TMX 125 Alpha STD");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     
            
            loJSON = record.getModel().setCategoryFirstLevelId("0010");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     
            
            loJSON = record.getModel().setBrandId("00001");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     
            
            loJSON = record.getModel().setModelId("00004");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     
            
            loJSON = record.getModel().setColorId("00001");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     
            
            loJSON = record.getModel().setVariantId("00004");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     
            
            loJSON = record.getModel().setInventoryTypeId("0002");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     
            
            loJSON = record.getModel().setIndustryCode("06");
            if ("error".equals((String) loJSON.get("result"))) {
                Assert.fail((String) loJSON.get("message"));
            }     

            try {
                loJSON = record.saveRecord();
            } catch (CloneNotSupportedException ex) {
                Logger.getLogger(testInventory.class.getName()).log(Level.SEVERE, null, ex);
            }
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
