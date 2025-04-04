
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
    static PurchaseOrderReceiving poPurchaseReceivingController;

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("sys.default.path.metadata", "D:/GGC_Maven_Systems/config/metadata/new/");

        instance = MiscUtil.Connect();
        
        poPurchaseReceivingController = new PurchaseOrderReceivingControllers(instance, null).PurchaseOrderReceiving();
    }

//    @Test
    public void testNewTransaction() {
        String branchCd = instance.getBranchCode();
        String industryId = "02";
        String remarks = "this is a test RSIE Class 3.";
        
        String stockId = "M00125000001";
        int quantity = 110;
        
        JSONObject loJSON;
        
        try {
            loJSON = poPurchaseReceivingController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            loJSON = poPurchaseReceivingController.NewTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            try {
                
                poPurchaseReceivingController.Master().setIndustryId(industryId); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getIndustryId(), industryId);
                poPurchaseReceivingController.Master().setTransactionDate(instance.getServerDate()); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getTransactionDate(), instance.getServerDate());
                poPurchaseReceivingController.Master().setReferenceDate(instance.getServerDate()); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getReferenceDate(), instance.getServerDate());
                poPurchaseReceivingController.Master().setReferenceNo("01"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getReferenceNo(), "01");
                poPurchaseReceivingController.Master().setCompanyId("0002"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getCompanyId(), "0002");
                poPurchaseReceivingController.Master().setTruckingId("C00124000010"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getTruckingId(), "C00124000010");
                poPurchaseReceivingController.Master().setTermCode("0000003"); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getTermCode(), "0000003");
                poPurchaseReceivingController.Master().setBranchCode(branchCd); //direct assignment of value
                Assert.assertEquals(poPurchaseReceivingController.Master().getBranchCode(), branchCd);

                //you can use poPurchaseReceivingController.SearchIndustry() when on UI 
    //            loJSON = poPurchaseReceivingController.SearchIndustry("", false);
    //            if (!"success".equals((String) loJSON.get("result"))){
    //                System.err.println((String) loJSON.get("message"));
    //                Assert.fail();
    //            } 
                
                poPurchaseReceivingController.Master().setRemarks(remarks);
                Assert.assertEquals(poPurchaseReceivingController.Master().getRemarks(), remarks);

                poPurchaseReceivingController.Detail(0).setStockId(stockId);
                poPurchaseReceivingController.Detail(0).setQuantity(quantity);
//                poPurchaseReceivingController.Detail(0).setExpiryDate(instance.getServerDate()); //direct assignment of value

                poPurchaseReceivingController.AddDetail();
                poPurchaseReceivingController.Detail(1).setStockId("M00225000111");
                poPurchaseReceivingController.Detail(1).setQuantity(0);
//                poPurchaseReceivingController.Detail(1).setExpiryDate(instance.getServerDate()); //direct assignment of value

                poPurchaseReceivingController.AddDetail();
                poPurchaseReceivingController.Detail(2).setStockId("M00225000222");
                poPurchaseReceivingController.Detail(2).setQuantity(1);
//                poPurchaseReceivingController.Detail(2).setExpiryDate(instance.getServerDate()); //direct assignment of value

                poPurchaseReceivingController.AddDetail();
                poPurchaseReceivingController.Detail(3).setStockId("M00225000333");
                poPurchaseReceivingController.Detail(3).setQuantity(5);
//                poPurchaseReceivingController.Detail(3).setExpiryDate(instance.getServerDate()); //direct assignment of value

                poPurchaseReceivingController.AddDetail();
                
                poPurchaseReceivingController.computeFields();
                
                //populate POR Serial
                loJSON = poPurchaseReceivingController.getPurchaseOrderReceivingSerial(3);
                if("success".equals((String) loJSON.get("result"))){
                    System.out.println("inv serial cnt" + poPurchaseReceivingController.getPurchaseOrderReceivingSerialCount());
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setSerial01("0011");
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setSerial02("0013");
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setPlateNo("001sa1");
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setConductionStickerNo("333");
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setLocationId("333");
                    poPurchaseReceivingController.PurchaseOrderReceivingSerialList(0).setStockId("333");
                }
                
                System.out.println("Industry ID : " + instance.getIndustry());
                System.out.println("Industry : " + poPurchaseReceivingController.Master().Industry().getDescription());
                System.out.println("TransNox : " + poPurchaseReceivingController.Master().getTransactionNo());
                
                loJSON = poPurchaseReceivingController.SaveTransaction();
                if (!"success".equals((String) loJSON.get("result"))) {
                    System.err.println((String) loJSON.get("message"));
                    Assert.fail();
                }
            
            } catch (SQLException | GuanzonException ex) {
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
        
        loJSON = poPurchaseReceivingController.InitTransaction();
        if (!"success".equals((String) loJSON.get("result"))){
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        poPurchaseReceivingController.Master().setIndustryId(industryId); //direct assignment of value
        poPurchaseReceivingController.Master().setCompanyId(companyId); //direct assignment of value
        poPurchaseReceivingController.Master().setSupplierId(supplierId); //direct assignment of value
        
        
        loJSON = poPurchaseReceivingController.getApprovedPurchaseOrder();
        if (!"success".equals((String) loJSON.get("result"))) {
            System.err.println((String) loJSON.get("message"));
            Assert.fail();
        }
        
        //retreiving using column index
        for (int lnCtr = 0; lnCtr <= poPurchaseReceivingController.getPurchaseOrderCount() - 1; lnCtr++){
            try {
                System.out.println("PO Row No ->> " + lnCtr);
                System.out.println("PO Transaction No ->> " + poPurchaseReceivingController.PurchaseOrderList(lnCtr).getTransactionNo());
                System.out.println("PO Transaction Date ->> " + poPurchaseReceivingController.PurchaseOrderList(lnCtr).getTransactionDate());
                System.out.println("PO Industry ->> " + poPurchaseReceivingController.PurchaseOrderList(lnCtr).Industry().getDescription());
                System.out.println("PO Company ->> " + poPurchaseReceivingController.PurchaseOrderList(lnCtr).Company().getCompanyName());
                System.out.println("PO Supplier ->> " + poPurchaseReceivingController.PurchaseOrderList(lnCtr).Supplier().getCompanyName());
                System.out.println("----------------------------------------------------------------------------------");
            } catch (GuanzonException | SQLException ex) {
                Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
//     @Test
    public void testOpenTransaction() {
        JSONObject loJSON;
        
        try {
            loJSON = poPurchaseReceivingController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = poPurchaseReceivingController.OpenTransaction("M00125000002");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Master().getColumnCount(); lnCol++){
                System.out.println(poPurchaseReceivingController.Master().getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(poPurchaseReceivingController.Master().Branch().getBranchName());
            System.out.println(poPurchaseReceivingController.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= poPurchaseReceivingController.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(poPurchaseReceivingController.Detail(lnCtr).getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Detail(lnCtr).getValue(lnCol));
                }
            }
        } catch (CloneNotSupportedException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }   
    
    @Test
    public void testUpdateTransaction() {
        JSONObject loJSON;
       
        try {
            loJSON = poPurchaseReceivingController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = poPurchaseReceivingController.OpenTransaction("M00125000001");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = poPurchaseReceivingController.UpdateTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            //Populate purhcase receiving serials
            for(int lnCtr = 0; lnCtr <= poPurchaseReceivingController.getDetailCount()-1; lnCtr++){
                poPurchaseReceivingController.getPurchaseOrderReceivingSerial(poPurchaseReceivingController.Detail(lnCtr).getEntryNo());
            }

//            poPurchaseReceivingController.Detail(1).setQuantity(0);
//            poPurchaseReceivingController.AddDetail();

            for(int lnCtr = 0;lnCtr <= poPurchaseReceivingController.getDetailCount()-1; lnCtr++){
                System.out.println("DATA before save Transation");
                System.out.println("OrderNo : " + lnCtr + " : " + poPurchaseReceivingController.Detail(lnCtr).getOrderNo());
                System.out.println("StockId : " + lnCtr + " : " + poPurchaseReceivingController.Detail(lnCtr).getStockId());
                System.out.println("---------------------------------------------------------------------");
            }

            loJSON = poPurchaseReceivingController.SaveTransaction();
            if (!"success".equals((String) loJSON.get("result"))) {
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            }
        } catch (CloneNotSupportedException | SQLException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, MiscUtil.getException(ex), ex);
        }
        
    }   
    
//    @Test
    public void testConfirmTransaction() {
        JSONObject loJSON;
        
        try {
            loJSON = poPurchaseReceivingController.InitTransaction();
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            loJSON = poPurchaseReceivingController.OpenTransaction("M00125000048");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 

            //retreiving using column index
            for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Master().getColumnCount(); lnCol++){
                System.out.println(poPurchaseReceivingController.Master().getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Master().getValue(lnCol));
            }
            //retreiving using field descriptions
            System.out.println(poPurchaseReceivingController.Master().Branch().getBranchName());
            System.out.println(poPurchaseReceivingController.Master().Category().getDescription());

            //retreiving using column index
            for (int lnCtr = 0; lnCtr <= poPurchaseReceivingController.Detail().size() - 1; lnCtr++){
                for (int lnCol = 1; lnCol <= poPurchaseReceivingController.Detail(lnCtr).getColumnCount(); lnCol++){
                    System.out.println(poPurchaseReceivingController.Detail(lnCtr).getColumn(lnCol) + " ->> " + poPurchaseReceivingController.Detail(lnCtr).getValue(lnCol));
                }
            }
            
            loJSON = poPurchaseReceivingController.ConfirmTransaction("test");
            if (!"success".equals((String) loJSON.get("result"))){
                System.err.println((String) loJSON.get("message"));
                Assert.fail();
            } 
            
            System.out.println((String) loJSON.get("message"));
        } catch (CloneNotSupportedException |ParseException e) {
            System.err.println(MiscUtil.getException(e));
            Assert.fail();
        } catch (SQLException | GuanzonException ex) {
            Logger.getLogger(testPurchaseOrderReceivingMaster.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        
    }   
    
}
