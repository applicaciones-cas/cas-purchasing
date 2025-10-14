
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.soap.Detail;
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

//    @Test
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
        } catch (SQLException ex) {
            Logger.getLogger(testPurchaseOrderReturn.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
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

            loJSON = PurchaseReturnController.OpenTransaction("M00125000001");
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
    
//    @Test
    public void testOpenTransaction() {
        JSONObject loJSON;
        
        try {
            loJSON = PurchaseReturnController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = PurchaseReturnController.OpenTransaction("M00125000001");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= PurchaseReturnController.Master().getColumnCount(); lnCol++){
                System.out.println(PurchaseReturnController.Master().getColumn(lnCol) + " ->> " + PurchaseReturnController.Master().getValue(lnCol));
            }
            
            //retreiving using field descriptions
            System.out.println(PurchaseReturnController.Master().Branch().getBranchName());
            System.out.println(PurchaseReturnController.Master().Category().getDescription());
            System.out.println(PurchaseReturnController.Master().PurchaseOrderReceivingMaster().getReferenceNo());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= PurchaseReturnController.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= PurchaseReturnController.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(PurchaseReturnController.Detail(lnCtr).getColumn(lnCol) + " ->> " + PurchaseReturnController.Detail(lnCtr).getValue(lnCol));
                }
                
                System.out.println("Receive Qty : " + PurchaseReturnController.getReceiveQty(lnCtr));
       
            }
            
        } catch (CloneNotSupportedException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReturn.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   
    
//    @Test
    public void testgetPurchaseReturnList() {
        String industryId = "01";
        String companyId = "0002";
        String categoryCd = "0001";
        String supplierId = "C00124000020";
        
        JSONObject loJSON;
        
        loJSON = PurchaseReturnController.InitTransaction();
        if (!"success".equals((String) loJSON.get("result"))){
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        PurchaseReturnController.Master().setIndustryId(industryId); //direct assignment of value
        PurchaseReturnController.Master().setCompanyId(companyId); //direct assignment of value
        PurchaseReturnController.Master().setSupplierId(supplierId); //direct assignment of value
        PurchaseReturnController.setIndustryId(industryId); //direct assignment of value
        PurchaseReturnController.setCompanyId(companyId); //direct assignment of value
        PurchaseReturnController.setCategoryId(categoryCd); //direct assignment of value
        
        
        loJSON = PurchaseReturnController.loadPurchaseOrderReturn("confirmation", supplierId, "");
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        //retreiving using column index
        for (int lnCtr = 0; lnCtr <= PurchaseReturnController.getPurchaseOrderReturnCount()- 1; lnCtr++){
            try {
                System.out.println("PO Row No ->> " + lnCtr);
                System.out.println("PO Transaction No ->> " + PurchaseReturnController.PurchaseOrderReturnList(lnCtr).getTransactionNo());
                System.out.println("PO Transaction Date ->> " + PurchaseReturnController.PurchaseOrderReturnList(lnCtr).getTransactionDate());
                System.out.println("PO Industry ->> " + PurchaseReturnController.PurchaseOrderReturnList(lnCtr).Industry().getDescription());
                System.out.println("PO Company ->> " + PurchaseReturnController.PurchaseOrderReturnList(lnCtr).Company().getCompanyName());
                System.out.println("PO Supplier ->> " + PurchaseReturnController.PurchaseOrderReturnList(lnCtr).Supplier().getCompanyName());
                System.out.println("----------------------------------------------------------------------------------");
            } catch (GuanzonException | SQLException ex) {
                Logger.getLogger(testPurchaseOrderReturn.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
//    @Test
    public void testConfirmTransaction() {
        JSONObject loJSON;
        
        try {
            loJSON = PurchaseReturnController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = PurchaseReturnController.OpenTransaction("M00125000001");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= PurchaseReturnController.Master().getColumnCount(); lnCol++){
                System.out.println(PurchaseReturnController.Master().getColumn(lnCol) + " ->> " + PurchaseReturnController.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(PurchaseReturnController.Master().Branch().getBranchName());
            System.out.println(PurchaseReturnController.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= PurchaseReturnController.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= PurchaseReturnController.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(PurchaseReturnController.Detail(lnCtr).getColumn(lnCol) + " ->> " + PurchaseReturnController.Detail(lnCtr).getValue(lnCol));
                }
            }
            
            loJSON = PurchaseReturnController.ConfirmTransaction("test confirm");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            System.out.println((String) loJSON.get("message"));
        } catch (CloneNotSupportedException |ParseException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReturn.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   
    
//    @Test
    public void testReturnTransaction() {
        JSONObject loJSON;
        
        try {
            loJSON = PurchaseReturnController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = PurchaseReturnController.OpenTransaction("M00125000001");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= PurchaseReturnController.Master().getColumnCount(); lnCol++){
                System.out.println(PurchaseReturnController.Master().getColumn(lnCol) + " ->> " + PurchaseReturnController.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(PurchaseReturnController.Master().Branch().getBranchName());
            System.out.println(PurchaseReturnController.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= PurchaseReturnController.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= PurchaseReturnController.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(PurchaseReturnController.Detail(lnCtr).getColumn(lnCol) + " ->> " + PurchaseReturnController.Detail(lnCtr).getValue(lnCol));
                }
            }
            
            loJSON = PurchaseReturnController.ReturnTransaction("test return");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            System.out.println((String) loJSON.get("message"));
        } catch (CloneNotSupportedException |ParseException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReturn.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   
    
//    @Test
    public void testVoidTransaction() {
        JSONObject loJSON;
        
        try {
            loJSON = PurchaseReturnController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = PurchaseReturnController.OpenTransaction("M00125000001");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= PurchaseReturnController.Master().getColumnCount(); lnCol++){
                System.out.println(PurchaseReturnController.Master().getColumn(lnCol) + " ->> " + PurchaseReturnController.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(PurchaseReturnController.Master().Branch().getBranchName());
            System.out.println(PurchaseReturnController.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= PurchaseReturnController.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= PurchaseReturnController.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(PurchaseReturnController.Detail(lnCtr).getColumn(lnCol) + " ->> " + PurchaseReturnController.Detail(lnCtr).getValue(lnCol));
                }
            }
            
            loJSON = PurchaseReturnController.VoidTransaction("test void");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            System.out.println((String) loJSON.get("message"));
        } catch (CloneNotSupportedException |ParseException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReturn.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   
    
    @AfterClass
    public static void tearDownClass() {
        PurchaseReturnController = null;
        instance = null;
    }
}