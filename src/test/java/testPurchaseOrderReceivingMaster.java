
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.appdriver.base.GuanzonException;
import org.guanzon.appdriver.base.MiscUtil;
import org.guanzon.cas.purchasing.controller.PurchaseOrderReceiving;
import org.guanzon.cas.purchasing.services.PurchaseOrderReceivingControllers;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Arsiela 03-12-2025
 */

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class testPurchaseOrderReceivingMaster {
    
    static GRiderCAS instance;
    static PurchaseOrderReceiving trans;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        instance = MiscUtil.Connect();
        
        trans = new PurchaseOrderReceivingControllers(instance, null).PurchaseOrderReceiving();
    }

    @Test
    public void testNewTransaction() {
        String branchCd = instance.getBranchCode();
        String industryId = "02";
        String categoryId = "0002";
        String remarks = "this is a test RSIE Class 3.";
        
        String stockId = "M00125000001";
        int quantity = 110;
        
        JSONObject loJSON;
        
        try {
            loJSON = trans.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            loJSON = trans.NewTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            try {
                trans.Master().setIndustryId(industryId); //direct assignment of value
                System.out.println("Industry ID : " + instance.getIndustry());
                System.out.println("Industry : " + trans.Master().Industry().getDescription());
                System.out.println("TransNox : " + trans.Master().getTransactionNo());
                
                trans.Master().setTransactionDate(instance.getServerDate()); //direct assignment of value
                Assert.assertEquals(trans.Master().getTransactionDate(), instance.getServerDate());
                trans.Master().setCompanyId("0002"); //direct assignment of value
                Assert.assertEquals(trans.Master().getTransactionDate(), instance.getServerDate());
                
//                trans.Master().setRefernceDate(instance.getServerDate()); //direct assignment of value
//                Assert.assertEquals(trans.Master().getRefernceDate(), instance.getServerDate());
//                
//                trans.Master().setDueDate(instance.getServerDate()); //direct assignment of value
//                Assert.assertEquals(trans.Master().getDueDate(), instance.getServerDate());
//                
//                trans.Master().setTermDueDate(instance.getServerDate()); //direct assignment of value
//                Assert.assertEquals(trans.Master().getTermDueDate(), instance.getServerDate());
            
                //you can use trans.SearchBranch() when on UI 
    //            loJSON = trans.SearchBranch("", false);
    //            if (!"success".equals((String) loJSON.get("result"))){
    //                System.err.println((String) loJSON.get("message"));
    //                Assert.fail();
    //            } 
                trans.Master().setBranchCode(branchCd); //direct assignment of value
                Assert.assertEquals(trans.Master().getBranchCode(), branchCd);

                //you can use trans.SearchIndustry() when on UI 
    //            loJSON = trans.SearchIndustry("", false);
    //            if (!"success".equals((String) loJSON.get("result"))){
    //                System.err.println((String) loJSON.get("message"));
    //                Assert.fail();
    //            } 
                trans.Master().setIndustryId(industryId); //direct assignment of value
                Assert.assertEquals(trans.Master().getIndustryId(), industryId);

                //you can use trans.SearchCategory() when on UI 
    //            loJSON = trans.SearchCategory("", false);
    //            if (!"success".equals((String) loJSON.get("result"))){
    //                System.err.println((String) loJSON.get("message"));
    //                Assert.fail();
    //            } 
    //            trans.Master().setCategoryId(categoryId); //direct assignment of value
    //            Assert.assertEquals(trans.Master().getCategoryId(), categoryId);

                trans.Master().setRemarks(remarks);
                Assert.assertEquals(trans.Master().getRemarks(), remarks);

                trans.Detail(0).setStockId(stockId);
                trans.Detail(0).setQuantity(quantity);
//                trans.Detail(0).setExpiryDate(instance.getServerDate()); //direct assignment of value

                trans.AddDetail();
                trans.Detail(1).setStockId("M00225000111");
                trans.Detail(1).setQuantity(0);
//                trans.Detail(1).setExpiryDate(instance.getServerDate()); //direct assignment of value

                trans.AddDetail();
                trans.Detail(2).setStockId("M00225000222");
                trans.Detail(2).setQuantity(1);
//                trans.Detail(2).setExpiryDate(instance.getServerDate()); //direct assignment of value

                trans.AddDetail();
                trans.Detail(3).setStockId("M00225000333");
                trans.Detail(3).setQuantity(5);
//                trans.Detail(3).setExpiryDate(instance.getServerDate()); //direct assignment of value

                trans.AddDetail();
                
                System.out.println("unit price" + trans.Detail(trans.getDetailCount() - 1).getUnitPrce());
                
                //populate POR Serial
                loJSON = trans.getPurchaseOrderReceivingSerial(2);
                if("success".equals((String) loJSON.get("result"))){
                    trans.PurchaseOrderReceivingSerialList(0).setSerial01("0011");
                    trans.PurchaseOrderReceivingSerialList(0).setSerial02("0013");
                    trans.PurchaseOrderReceivingSerialList(0).setPlateNo("001sa1");
                }
                
                loJSON = trans.SaveTransaction();
                if (!"success".equals((String) loJSON.get("result"))) {
                    System.err.println((String) loJSON.get("message"));
                    Assert.fail();
                }
            
            } catch (SQLException ex) {
                Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
            } catch (GuanzonException ex) {
                Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (ExceptionInInitializerError e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (CloneNotSupportedException ex) {
            Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
//    @Test
    public void testgetPurchaseOrderList() {
        String industryId = "02";
        String companyId = "0001";
        String supplierId = "C00124000009";
        
        JSONObject loJSON;
        
        loJSON = trans.InitTransaction();
        if (!"success".equals((String) loJSON.get("result"))){
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        trans.Master().setIndustryId(industryId); //direct assignment of value
        trans.Master().setCompanyId(companyId); //direct assignment of value
        trans.Master().setSupplierId(supplierId); //direct assignment of value
        
        
        loJSON = trans.getApprovedPurchaseOrder();
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        //retreiving using column index
        for (int lnCtr = 0; lnCtr <= trans.getPurchaseOrderCount() - 1; lnCtr++){
            try {
                System.out.println("PO Row No ->> " + lnCtr);
                System.out.println("PO Transaction No ->> " + trans.PurchaseOrderList(lnCtr).getTransactionNo());
                System.out.println("PO Transaction Date ->> " + trans.PurchaseOrderList(lnCtr).getTransactionDate());
                System.out.println("PO Industry ->> " + trans.PurchaseOrderList(lnCtr).Industry().getDescription());
                System.out.println("PO Company ->> " + trans.PurchaseOrderList(lnCtr).Company().getCompanyName());
                System.out.println("PO Supplier ->> " + trans.PurchaseOrderList(lnCtr).Supplier().getCompanyName());
                System.out.println("----------------------------------------------------------------------------------");
            } catch (GuanzonException ex) {
                Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SQLException ex) {
                Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
//     @Test
    public void testOpenTransaction() {
        JSONObject loJSON;
        
        try {
            loJSON = trans.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = trans.OpenTransaction("M00125000002");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= trans.Master().getColumnCount(); lnCol++){
                System.out.println(trans.Master().getColumn(lnCol) + " ->> " + trans.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(trans.Master().Branch().getBranchName());
            System.out.println(trans.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= trans.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= trans.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(trans.Detail(lnCtr).getColumn(lnCol) + " ->> " + trans.Detail(lnCtr).getValue(lnCol));
                }
            }
        } catch (CloneNotSupportedException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException ex) {
            Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }   
    
//    @Test
    public void testUpdateTransaction() {
        JSONObject loJSON;
       
        try {
            loJSON = trans.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = trans.OpenTransaction("M00125000003");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = trans.UpdateTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            trans.Detail(1).setQuantity(0);
            trans.AddDetail();

            loJSON = trans.SaveTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
        } catch (CloneNotSupportedException | SQLException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }   
    
//    @Test
    public void testConfirmTransaction() {
        JSONObject loJSON;
        
        try {
            loJSON = trans.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = trans.OpenTransaction("M00125000003");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= trans.Master().getColumnCount(); lnCol++){
                System.out.println(trans.Master().getColumn(lnCol) + " ->> " + trans.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(trans.Master().Branch().getBranchName());
            System.out.println(trans.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= trans.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= trans.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(trans.Detail(lnCtr).getColumn(lnCol) + " ->> " + trans.Detail(lnCtr).getValue(lnCol));
                }
            }
            
            loJSON = trans.ConfirmTransaction("");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            System.out.println((String) loJSON.get("message"));
        } catch (CloneNotSupportedException |ParseException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException ex) {
            Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
        } catch (GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }   
    
}
