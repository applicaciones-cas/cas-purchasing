

import java.math.BigDecimal;
import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.base.SQLUtil;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

//public class testPurchaseOrderPosting {
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
public class testPurchaseOrderPosting {
 
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
        String lsfsTransNox="M00124000052";

        loJSON = record.openTransaction(lsfsTransNox);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        //setting testing
        loJSON = record.postTransaction(lsfsTransNox);
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        loJSON = record.saveTransaction();
        if ("error".equals((String) loJSON.get("result"))) {
            Assert.fail((String) loJSON.get("message"));
        }
        
        
        //for searching detail
        // record.searchDetail(0, "sStockIdx", "mc", true);

    }

    @AfterClass
    public static void tearDownClass() {
        record = null;
        instance = null;
    }
}
