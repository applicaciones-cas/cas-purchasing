import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.appdriver.constant.TransactionStatus;
import org.guanzon.cas.purchasing.controller.PurchaseOrder;
import org.json.simple.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author User
 */

    
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPurchaseOrderConfirm {
 
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
        String lsfsTransNox="M00124000054";

        loJSON = record.closeTransaction(lsfsTransNox);
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
