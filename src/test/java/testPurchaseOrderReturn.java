
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.purchasing.controller.PurchaseOrderReturn;
import org.guanzon.cas.purchasing.services.PurchaseOrderControllers;
import org.guanzon.cas.purchasing.services.PurchaseOrderReturnControllers;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPurchaseOrderReturn {

    static GRiderCAS instance;
    static PurchaseOrderReturn PurchaseReturnController;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        instance = MiscUtil.Connect();
        
        PurchaseReturnController = new PurchaseOrderReturnControllers(instance, null).PurchaseOrderReturn();
    }

    @Test
    public void testNewTransaction() {
        String branchCd = instance.getBranchCode();
        String industryId = "01";
        String remarks = "this is a test RSIE PO Return.";
        String stockId = "C0W125000001";
        int quantity = 1;
        
        JSONObject loJSON;
        
        try {
            
            loJSON = PurchaseReturnController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            loJSON = PurchaseReturnController.NewTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            try {
                PurchaseReturnController.setIndustryId(industryId);
                PurchaseReturnController.setCompanyId("0002");
                PurchaseReturnController.setCategoryId("0001");
                
                PurchaseReturnController.initFields();
                PurchaseReturnController.Master().setCategoryCode("0001"); //direct assignment of value
                Assert.assertEquals(PurchaseReturnController.Master().getCategoryCode(), "0001");
                PurchaseReturnController.Master().setIndustryId(industryId); //direct assignment of value
                Assert.assertEquals(PurchaseReturnController.Master().getIndustryId(), industryId);
                PurchaseReturnController.Master().setTransactionDate(instance.getServerDate()); //direct assignment of value
                Assert.assertEquals(PurchaseReturnController.Master().getTransactionDate(), instance.getServerDate());
                PurchaseReturnController.Master().setCompanyId("0002"); //direct assignment of value
                Assert.assertEquals(PurchaseReturnController.Master().getCompanyId(), "0002");
                PurchaseReturnController.Master().setSupplierId("C00124000020"); //direct assignment of value
                Assert.assertEquals(PurchaseReturnController.Master().getSupplierId(), "C00124000020");
                PurchaseReturnController.Master().setBranchCode(branchCd); //direct assignment of value
                Assert.assertEquals(PurchaseReturnController.Master().getBranchCode(), branchCd);
                
                PurchaseReturnController.Master().setSourceNo("M00125000001"); //direct assignment of value
                Assert.assertEquals(PurchaseReturnController.Master().getSourceNo(), "M00125000001");
                
                PurchaseReturnController.Master().setRemarks(remarks);
                Assert.assertEquals(PurchaseReturnController.Master().getRemarks(), remarks);

                PurchaseReturnController.Detail(0).setStockId(stockId);
                PurchaseReturnController.Detail(0).setQuantity(quantity);
                PurchaseReturnController.Detail(0).setQuantity(1);
                PurchaseReturnController.AddDetail();
                
                PurchaseReturnController.computeFields();
                
                System.out.println("Industry ID : " + instance.getIndustry());
                System.out.println("Industry : " + PurchaseReturnController.Master().Industry().getDescription());
                System.out.println("TransNox : " + PurchaseReturnController.Master().getTransactionNo());
                
                loJSON = PurchaseReturnController.SaveTransaction();
                if (!"success".equals((String) loJSON.get("result"))) {
                    System.err.println((String) loJSON.get("message"));
                    Assert.fail();
                }
            
            } catch (SQLException | GuanzonException ex) {
                Logger.getLogger(testPurchaseOrderReturn.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ExceptionInInitializerError e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(testPurchaseOrderReturn.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
//    @Test
    public void testUpdateTransaction() {
        JSONObject loJSON;
       
        try {
            loJSON = PurchaseReturnController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = PurchaseReturnController.OpenTransaction("M00125000006");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = PurchaseReturnController.UpdateTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }

            loJSON = PurchaseReturnController.SaveTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
        } catch (CloneNotSupportedException | SQLException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReturn.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        
    }   
    
    @AfterClass
    public static void tearDownClass() {
        PurchaseReturnController = null;
        instance = null;
    }
}