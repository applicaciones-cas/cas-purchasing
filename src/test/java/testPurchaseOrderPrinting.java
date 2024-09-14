

import java.math.BigDecimal;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.guanzon.cas.purchasing.testing.*;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

//public class testPurchaseOrderPrinting {
    //creare
    //save
    
    //update
    //save
    
    //closetransaction(printing)
//}

//
//public class testPurchaseOrderNew {
//    //1. Matched yung values na pinass natin
//    //2. Check kung yung nasave sa database after saving is matched sa mga pinass
//    //3. Delete yung mga sinave
//    
//    
//    
//    
//}

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPurchaseOrderPrinting {
 
    static GRider instance;
    static PurchaseOrder record;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/");

        instance = MiscUtil.Connect();
        record = new PurchaseOrder(instance, false);
    }

    @Test
    public void testProgramFlow() {
        JSONObject loJSON;

        //setting testing
        loJSON = record.closeTransaction("M00123000001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        //set detail information
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setStockID("M00124000001");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setDescription("This is Detail 02");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setQtyOnHand(0);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setROQQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setOrderQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setUnitPrice(BigDecimal.valueOf(1800.19));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setReceiveNo(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setCancelledNo(0);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setModifiedDate(instance.getServerDate());
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        //set detail 2 information
        record.AddModelDetail();
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setStockID("M00124000002");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setDescription("This is Detail 03 entry no2");
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setQtyOnHand(0);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setROQQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setOrderQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setQuantity(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setUnitPrice(BigDecimal.valueOf(1800.19));
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setReceiveNo(1);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setCancelledNo(0);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        loJSON = record.getDetailModel().get(record.getDetailModel().size()-1).setModifiedDate(instance.getServerDate());
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        
        
        loJSON = record.saveTransaction();
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        
        
        
        
    }

    @AfterClass
    public static void tearDownClass() {
        record = null;
        instance = null;
    }
}
