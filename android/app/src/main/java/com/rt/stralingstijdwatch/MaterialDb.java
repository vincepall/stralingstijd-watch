package com.rt.stralingstijdwatch;

public class MaterialDb {
    public static class Material {
        public final String name;
        public final String werkstoff;
        public final String group;
        public final double factorSe75;
        public final double factorIr192;

        public Material(String name, String werkstoff, String group, double factorSe75, double factorIr192) {
            this.name = name;
            this.werkstoff = werkstoff;
            this.group = group;
            this.factorSe75 = factorSe75;
            this.factorIr192 = factorIr192;
        }

        public double getFactor(String sourceType) {
            if ("Se75".equalsIgnoreCase(sourceType)) {
                return factorSe75;
            }
            return factorIr192;
        }
    }

    public static final Material[] ALL = new Material[] {
        new Material("Hastelloy C-276", "2.4819 / UNS N10276", "Hastelloy", 3.40, 1.79),
        new Material("Hastelloy C-22", "2.4602 / UNS N06022", "Hastelloy", 2.67, 1.62),
        new Material("Hastelloy C-4", "2.4610 / UNS N06455", "Hastelloy", 1.99, 1.43),
        new Material("Hastelloy C-2000", "2.4675 / UNS N06200", "Hastelloy", 1.93, 1.40),
        new Material("Hastelloy B-2", "2.4617 / UNS N10665", "Hastelloy", 3.08, 1.75),
        new Material("Hastelloy B-3", "2.4600 / UNS N10675", "Hastelloy", 3.56, 1.82),
        new Material("Hastelloy X", "2.4665 / UNS N06002", "Hastelloy", 1.53, 1.24),
        new Material("Hastelloy G-30", "2.4603 / UNS N06030", "Hastelloy", 1.81, 1.34),
        new Material("Hastelloy G-35", "2.4643 / UNS N06035", "Hastelloy", 1.42, 1.20),
        new Material("Hastelloy N", "2.4607 / UNS N10003", "Hastelloy", 2.10, 1.46),
        new Material("Hastelloy W", "2.4612 / UNS N10004", "Hastelloy", 2.57, 1.59),
        new Material("Inconel 625 (Alloy 625)", "2.4856 / UNS N06625", "Inconel", 1.72, 1.32),
        new Material("Inconel 718 (Alloy 718)", "2.4668 / UNS N07718", "Inconel", 1.41, 1.18),
        new Material("Inconel 600 (Alloy 600)", "2.4816 / UNS N06600", "Inconel", 1.27, 1.15),
        new Material("Inconel 601 (Alloy 601)", "2.4851 / UNS N06601", "Inconel", 1.15, 1.08),
        new Material("Inconel 617 (Alloy 617)", "2.4663 / UNS N06617", "Inconel", 1.53, 1.25),
        new Material("Inconel 686 (Alloy 686)", "2.4606 / UNS N06686", "Inconel", 3.10, 1.70),
        new Material("Inconel 690 (Alloy 690)", "2.4642 / UNS N06690", "Inconel", 1.15, 1.08),
        new Material("Inconel X-750", "2.4669 / UNS N07750", "Inconel", 1.30, 1.16),
        new Material("Inconel 725 (Alloy 725)", "UNS N07725", "Inconel", 1.57, 1.26),
        new Material("Incoloy 800 / 800H (Alloy 800)", "1.4876 / UNS N08800/N08810", "Incoloy", 1.07, 1.04),
        new Material("Incoloy 825 (Alloy 825)", "2.4858 / UNS N08825", "Incoloy", 1.22, 1.11),
        new Material("Incoloy 925 (Alloy 925)", "UNS N09925", "Incoloy", 1.22, 1.11),
        new Material("Incoloy A-286", "1.4980 / UNS S66286", "Incoloy", 1.07, 1.04),
        new Material("Incoloy 803", "UNS S35045", "Incoloy", 1.07, 1.04),
        new Material("Monel 400 (Alloy 400)", "2.4360 / UNS N04400", "Monel", 1.63, 1.32),
        new Material("Monel K-500 (Alloy K-500)", "2.4375 / UNS N05500", "Monel", 1.44, 1.22),
        new Material("Stellite 6 (CoCr-A)", "2.4994 / CoCr-A", "Kobaltbasis", 2.04, 1.41),
        new Material("Stellite 21 (CoCr-E)", "CoCr-E", "Kobaltbasis", 1.25, 1.13),
        new Material("Stellite 12 (CoCr-B)", "CoCr-B", "Kobaltbasis", 2.23, 1.36),
        new Material("Haynes 25 (Alloy L-605)", "2.4964 / UNS R30605", "Kobaltbasis", 2.26, 1.46),
        new Material("Haynes 188", "2.4683 / UNS R30188", "Kobaltbasis", 2.16, 1.36),
        new Material("MP35N", "2.4999 / UNS R30035", "Kobaltbasis", 1.57, 1.26),
        new Material("Titanium Grade 1 & 2 (CP Ti)", "3.7035 / UNS R50400", "Titanium", 0.71, 0.90),
        new Material("Titanium Grade 5 (Ti-6Al-4V)", "3.7164 / UNS R56400", "Titanium", 0.71, 0.90),
        new Material("Titanium Grade 7 (Ti-Pd)", "3.7235 / UNS R52400", "Titanium", 0.71, 0.90),
        new Material("Titanium Grade 9 (Ti-3Al-2.5V)", "3.7194 / UNS R56320", "Titanium", 0.71, 0.90),
        new Material("Titanium Grade 12 (Ti-Mo-Ni)", "3.7105 / UNS R53400", "Titanium", 0.71, 0.90),
        new Material("Koolstofstaal (Referentie)", "1.0460 / P250GH / C22", "RVS & Duplex", 1.00, 1.00),
        new Material("RVS 304 / 304L", "1.4301 / 1.4307 / 304L", "RVS & Duplex", 1.00, 1.00),
        new Material("RVS 316 / 316L", "1.4401 / 1.4404 / 316L", "RVS & Duplex", 1.08, 1.05),
        new Material("RVS 316Ti", "1.4571 / AISI 316Ti", "RVS & Duplex", 1.06, 1.03),
        new Material("RVS 321", "1.4541 / AISI 321", "RVS & Duplex", 0.98, 0.98),
        new Material("RVS 347", "1.4550 / AISI 347", "RVS & Duplex", 1.02, 1.01),
        new Material("RVS 310S", "1.4845 / AISI 310S", "RVS & Duplex", 0.99, 0.99),
        new Material("RVS 309S", "1.4828 / AISI 309S", "RVS & Duplex", 0.96, 0.99),
        new Material("Duplex 2205", "1.4462 / UNS S32205", "RVS & Duplex", 1.04, 1.01),
        new Material("Lean Duplex 2101", "1.4162 / UNS S32101", "RVS & Duplex", 0.92, 0.95),
        new Material("Lean Duplex 2304", "1.4362 / UNS S32304", "RVS & Duplex", 0.95, 0.96),
        new Material("Super Duplex 2507", "1.4410 / UNS S32750", "RVS & Duplex", 1.07, 1.04),
        new Material("Super Duplex Zeron 100", "1.4501 / UNS S32760", "RVS & Duplex", 1.17, 1.08),
        new Material("254 SMO (6Mo)", "1.4547 / UNS S31254", "RVS & Duplex", 1.21, 1.11),
        new Material("904L", "1.4539 / UNS N08904", "RVS & Duplex", 1.17, 1.09),
        new Material("Alloy 20 (Carpenter 20)", "2.4660 / UNS N08020", "RVS & Duplex", 1.21, 1.10),
        new Material("Sanicro 28 (Alloy 28)", "1.4563 / UNS N08028", "RVS & Duplex", 1.09, 1.03),
        new Material("Alloy 31", "1.4562 / UNS N08031", "RVS & Duplex", 1.22, 1.10),
        new Material("253 MA", "1.4835 / UNS S30815", "RVS & Duplex", 0.96, 0.97),
        new Material("17-4 PH (AISI 630)", "1.4542 / UNS S17400", "RVS & Duplex", 0.94, 0.97),
        new Material("16Mo3 (15Mo3)", "1.5415 / 16Mo3", "Ketelstaal", 1.00, 1.00),
        new Material("13CrMo4-5 (P11 / T11)", "1.7335 / A335 P11", "Ketelstaal", 1.00, 1.00),
        new Material("10CrMo9-10 (P22 / T22)", "1.7380 / A335 P22", "Ketelstaal", 1.02, 1.01),
        new Material("11CrMo9-10", "1.7383", "Ketelstaal", 1.00, 1.00),
        new Material("14MoV6-3", "1.7715", "Ketelstaal", 1.00, 1.00),
        new Material("X10CrMoVNb9-1 (P91 / T91)", "1.4903 / A335 P91", "Ketelstaal", 0.97, 0.98),
        new Material("X10CrWMoVNb9-2 (P92 / T92)", "1.4901 / A335 P92", "Ketelstaal", 1.24, 1.10),
        new Material("VM12-SHC (12Cr)", "1.4915", "Ketelstaal", 1.19, 1.09),
        new Material("Koper Zuiver (Cu-ETP / Cu-DHP)", "2.0060 / 2.0090 / CW004A", "Koper & Brons", 1.71, 1.33),
        new Material("Messing / Geelkoper (CuZn37)", "2.0321 / CW508L", "Koper & Brons", 1.59, 1.25),
        new Material("Messing (CuZn39Pb3)", "2.0401 / CW614N", "Koper & Brons", 2.57, 1.55),
        new Material("Tinbrons (CuSn8)", "2.1030 / CW453K", "Koper & Brons", 2.23, 1.47),
        new Material("Tinbrons (CuSn6)", "2.1020 / CW452K", "Koper & Brons", 2.08, 1.44),
        new Material("Roodbrons (Rg7 / CuSn7ZnPb)", "2.1090 / CC493K", "Koper & Brons", 3.42, 1.75),
        new Material("Aluminiumbrons (CuAl10Fe5Ni5)", "2.0966 / CW307G", "Koper & Brons", 1.11, 1.03),
        new Material("Cupro-nikkel 90/10", "2.0872 / CW352H", "Koper & Brons", 1.69, 1.32),
        new Material("Cupro-nikkel 70/30", "2.0882 / CW354H", "Koper & Brons", 1.71, 1.36),
        new Material("Nikkel 200 / 201 (Zuiver Nikkel)", "2.4066 / UNS N02200", "Exotisch & Speciaal", 1.65, 1.34),
        new Material("Zirkonium 702 (Zr 702)", "UNS R60702", "Exotisch & Speciaal", 2.00, 1.26),
        new Material("Zirkonium 705 (Zr 705)", "UNS R60705", "Exotisch & Speciaal", 2.00, 1.26),
        new Material("Tantaal (Zuiver Ta)", "2.4560 / UNS R05200", "Exotisch & Speciaal", 5.06, 2.94),
        new Material("Niobium (Columbium)", "UNS R04200", "Exotisch & Speciaal", 1.90, 1.34),
        new Material("Molybdeen (Puur Mo)", "UNS R03600", "Exotisch & Speciaal", 2.33, 1.46),
        new Material("Wolfraam (Tungsten)", "W 99.95%", "Exotisch & Speciaal", 6.32, 3.21),
        new Material("Lood (Pb)", "2.3020 / PB970R", "Exotisch & Speciaal", 4.66, 2.56),
        new Material("Tin (Sn)", "Sn 99.9%", "Exotisch & Speciaal", 2.06, 1.26),
        new Material("Zink (Zn)", "Zn 99.9%", "Exotisch & Speciaal", 1.27, 1.02),
        new Material("Aluminium Zuiver (1050A)", "3.0255 / EN AW-1050A", "Exotisch & Speciaal", 0.20, 0.35),
        new Material("Aluminium AlMg3", "3.3535 / EN AW-5754", "Exotisch & Speciaal", 0.20, 0.35),
        new Material("Aluminium AlMg4.5Mn", "3.3547 / EN AW-5083", "Exotisch & Speciaal", 0.20, 0.35),
        new Material("Magnesium Zuiver", "AZ31 / Zuiver Mg", "Exotisch & Speciaal", 0.08, 0.08),
    };
}
