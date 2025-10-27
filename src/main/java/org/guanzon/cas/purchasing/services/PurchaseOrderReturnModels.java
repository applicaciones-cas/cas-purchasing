/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.guanzon.cas.purchasing.services;

import org.guanzon.appdriver.base.GRiderCAS;
import ph.com.guanzongroup.cas.model.Model_POReturn_Detail;
import ph.com.guanzongroup.cas.model.Model_POReturn_Master;

/**
 *
 * @author Arsiela 03-12-2025
 */
public class PurchaseOrderReturnModels {
    
    private final GRiderCAS poGRider;
    private Model_POReturn_Master POMaster;
    private Model_POReturn_Detail PODetail;
    
    public PurchaseOrderReturnModels(GRiderCAS applicationDriver){
        poGRider = applicationDriver;
    }
    
    public Model_POReturn_Master PurchaseOrderReturnMaster(){
        if (poGRider == null){
            System.err.println("PurchaseOrderReturnModels.PurchaseOrderReturnMaster: Application driver is not set.");
            return null;
        }
        
        if (POMaster == null){
            POMaster = new Model_POReturn_Master();
            POMaster.setApplicationDriver(poGRider);
            POMaster.setXML("Model_PO_Return_Master");
            POMaster.setTableName("PO_Return_Master");
            POMaster.initialize();
        }

        return POMaster;
    }
    
    public Model_POReturn_Detail PurchaseOrderReturnDetails(){
        if (poGRider == null){
            System.err.println("PurchaseOrderReturnModels.PurchaseOrderReturnDetails: Application driver is not set.");
            return null;
        }
        
        if (PODetail == null){
            PODetail = new Model_POReturn_Detail();
            PODetail.setApplicationDriver(poGRider);
            PODetail.setXML("Model_PO_Return_Detail");
            PODetail.setTableName("PO_Return_Detail");
            PODetail.initialize();
        }

        return PODetail;
    }
    
}
