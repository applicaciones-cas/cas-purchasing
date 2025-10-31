package org.guanzon.cas.purchasing.services;

import org.guanzon.appdriver.base.GRiderCAS;
import org.guanzon.cas.purchasing.model.Model_PO_Cancellation_Detail;
import org.guanzon.cas.purchasing.model.Model_PO_Cancellation_Master;

/**
 *
 * @author 12mnv
 */
public class POModels {

    private Model_PO_Cancellation_Master poPOCancellationMaster;
    private Model_PO_Cancellation_Detail poPOCancellationDetail;

    private final GRiderCAS poGRider;

    public POModels(GRiderCAS applicationDriver) {
        this.poGRider = applicationDriver;
    }

    public Model_PO_Cancellation_Master POCancellationMaster() {
        if (this.poGRider == null) {
            System.err.println("POModels.POCancellationMaster: Application driver is not set.");
            return null;
        }
        if (this.poPOCancellationMaster == null) {
            this.poPOCancellationMaster = new Model_PO_Cancellation_Master();
            this.poPOCancellationMaster.setApplicationDriver(this.poGRider);
            this.poPOCancellationMaster.setXML("Model_PO_Cancellation_Master");
            this.poPOCancellationMaster.setTableName("PO_Cancellation_Master");
            this.poPOCancellationMaster.initialize();
        }
        return this.poPOCancellationMaster;
    }

    public Model_PO_Cancellation_Detail POCancellationDetail() {
        if (this.poGRider == null) {
            System.err.println("POModels.POCancellationDetail: Application driver is not set.");
            return null;
        }
        if (this.poPOCancellationDetail == null) {
            this.poPOCancellationDetail = new Model_PO_Cancellation_Detail();
            this.poPOCancellationDetail.setApplicationDriver(this.poGRider);
            this.poPOCancellationDetail.setXML("Model_PO_Cancellation_Detail");
            this.poPOCancellationDetail.setTableName("PO_Cancellation_Detail");
            this.poPOCancellationDetail.initialize();
        }
        return this.poPOCancellationDetail;
    }

}
