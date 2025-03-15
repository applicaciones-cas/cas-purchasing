/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.services;

import org.guanzon.appdriver.base.GRider;
import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.cas.purchasing.model.Model_POR_Detail;
import org.guanzon.cas.purchasing.model.Model_POR_Master;
import org.guanzon.cas.purchasing.model.Model_POR_Serial;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReceivingModels {
    
    private final GRiderCAS poGRider;
    private Model_POR_Master POMaster;
    private Model_POR_Detail PODetail;
    private Model_POR_Serial POSerial;
    
    public PurchaseOrderReceivingModels(GRiderCAS applicationDriver){
        poGRider = applicationDriver;
    }
    
    public Model_POR_Master PurchaseOrderReceivingMaster(){
        if (poGRider == null){
            System.err.println("PurchaseOrderReceivingModels.PurchaseOrderReceivingMaster: Application driver is not set.");
            return null;
        }
        
        if (POMaster == null){
            POMaster = new Model_POR_Master();
            POMaster.setApplicationDriver(poGRider);
            POMaster.setXML("Model_PO_Receiving_Master");
            POMaster.setTableName("PO_Receiving_Master");
            POMaster.initialize();
        }

        return POMaster;
    }
    
    public Model_POR_Detail PurchaseOrderReceivingDetails(){
        if (poGRider == null){
            System.err.println("PurchaseOrderReceivingModels.PurchaseOrderReceivingDetails: Application driver is not set.");
            return null;
        }
        
        if (PODetail == null){
            PODetail = new Model_POR_Detail();
            PODetail.setApplicationDriver(poGRider);
            PODetail.setXML("Model_PO_Receiving_Detail");
            PODetail.setTableName("PO_Receiving_Detail");
            PODetail.initialize();
        }

        return PODetail;
    }
    
    public Model_POR_Serial PurchaseOrderReceivingSerial(){
        if (poGRider == null){
            System.err.println("PurchaseOrderReceivingModels.PurchaseOrderReceivingSerial: Application driver is not set.");
            return null;
        }
        
        if (POSerial == null){
            POSerial = new Model_POR_Serial();
            POSerial.setApplicationDriver(poGRider);
            POSerial.setXML("Model_PO_Receiving_Serial");
            POSerial.setTableName("PO_Receiving_Serial");
            POSerial.initialize();
        }

        return POSerial;
    }
    
}
