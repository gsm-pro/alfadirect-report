package com.github.gsmpro.brokerage.xml;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "Report")
@Getter
public class Root implements Serializable {
    @XmlElement(name = "Trades")
    private final Root.Trades trades = new Root.Trades();

    @Getter
    public static class Trades implements Serializable {
        @XmlElement(name = "Report")
        private final Root.Trades.Report report = new Root.Trades.Report();

        @SuppressWarnings("unused")
        @Getter
        public static class Report implements Serializable {
            @XmlElement(name = "Tablix2")
            private final Root.Trades.Report.Table2 table2 = new Root.Trades.Report.Table2();
            @XmlElement(name = "Tablix3")
            private final Root.Trades.Report.Table3 table3 = new Root.Trades.Report.Table3();

            @Getter
            public static class Table2 implements Serializable {
                @XmlElement(name = "Details_Collection")
                private final Root.Trades.Report.Table2.RawDetails2 details = new Root.Trades.Report.Table2.RawDetails2();

                @Getter
                public static class RawDetails2 implements Serializable {
                    @XmlElement(name = "Details")
                    private final List<Root.Trades.Report.Table2.RawDetails2.RawDetail2> list = new ArrayList<>();

                    @Getter
                    public static class RawDetail2 implements Serializable {
                        @XmlAttribute(name = "db_time")
                        private String time;
                        @XmlAttribute(name = "place_name")
                        private String place;
                        @XmlAttribute(name = "p_name")
                        private String company;
                        @XmlAttribute(name = "Price")
                        private String price;
                        @XmlAttribute(name = "qty")
                        private String qty;
                        @XmlAttribute(name = "curr_calc")
                        private String currency;
                    }
                }
            }

            @Getter
            public static class Table3 implements Serializable {
                @XmlElement(name = "Details2_Collection")
                private final Root.Trades.Report.Table3.RawDetails3 details = new Root.Trades.Report.Table3.RawDetails3();

                @Getter
                public static class RawDetails3 implements Serializable {
                    @XmlElement(name = "Details2")
                    private final List<Root.Trades.Report.Table3.RawDetails3.RawDetail3> list = new ArrayList<>();

                    @Getter
                    public static class RawDetail3 implements Serializable {
                        @XmlAttribute(name = "db_time2")
                        private String time;
                        @XmlAttribute(name = "place_name2")
                        private String place;
                        @XmlAttribute(name = "p_name2")
                        private String company;
                        @XmlAttribute(name = "Price2")
                        private String price;
                        @XmlAttribute(name = "qty2")
                        private String qty;
                        @XmlAttribute(name = "curr_calc2")
                        private String currency;
                    }
                }
            }
        }
    }
}