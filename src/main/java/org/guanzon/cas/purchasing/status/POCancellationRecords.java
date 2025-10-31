package org.guanzon.cas.purchasing.status;

/**
 *
 * @author Maynard
 */
public class POCancellationRecords {
    public static final String PurchaseOrder() {
        String lsSQL = "SELECT"
                + "  a.sTransNox,"
                + "  a.sBranchCd,"
                + "  a.sIndstCdx,"
                + "  a.sCategrCd,"
                + "  a.dTransact,"
                + "  a.sCompnyID,"
                + "  a.sDestinat,"
                + "  a.sSupplier,"
                + "  a.sAddrssID,"
                + "  a.sContctID,"
                + "  a.sReferNox,"
                + "  a.sTermCode,"
                + "  a.nDiscount,"
                + "  a.nAddDiscx,"
                + "  a.nTranTotl,"
                + "  a.nAmtPaidx,"
                + "  a.cWithAddx,"
                + "  a.nDPRatexx,"
                + "  a.nAdvAmtxx,"
                + "  a.nNetTotal,"
                + "  a.sRemarksx,"
                + "  a.dExpected,"
                + "  a.cEmailSnt,"
                + "  a.nEmailSnt,"
                + "  a.cPrintxxx,"
                + "  a.nEntryNox,"
                + "  a.sInvTypCd,"
                + "  a.cPreOwned,"
                + "  a.cProcessd,"
                + "  a.cTranStat,"
                + "  b.nQuantity,"
                + "  b.nReceived,"
                + "  b.nCancelld,"
                + "  c.sBranchNm xDestinat,"
                + "  b.sStockIDx,"
                + "  d.sBarCodex,"
                + "  IFNULL(d.sDescript, b.sDescript) xDescript,"
                + "  IFNULL(e.sDescript, '') xBrandNme,"
                + "  IFNULL(f.sDescript, '') xModelNme,"
                + "  IFNULL(g.sDescript, '') xColorNme"
                + " FROM PO_Master a"
                + " LEFT JOIN PO_Detail b ON a.sTransNox = b.sTransNox"
                + " LEFT JOIN Branch c ON a.sDestinat = c.sBranchCd"
                + " LEFT JOIN Inventory d ON b.sStockIDx = d.sStockIDx"
                + " LEFT JOIN Brand e ON d.sBrandIDx = e.sBrandIDx"
                + " LEFT JOIN Model f ON d.sModelIDx = f.sModelIDx"
                + " LEFT JOIN Color g ON d.sColorIDx = g.sColorIDx";

        return lsSQL;
    }
}
