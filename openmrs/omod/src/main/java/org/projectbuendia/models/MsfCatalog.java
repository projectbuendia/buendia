package org.projectbuendia.models;

public interface MsfCatalog extends Catalog {
    Route UNSPECIFIED = new Route("", "", "");
    Route PO = new Route("PO", "oral [fr:orale]", "PO");
    Route IV = new Route("IV", "intravenous [fr:intraveineuse]", "IV");
    Route SC = new Route("SC", "subcutaneous [fr:sous-cutanée", "SC");
    Route IM = new Route("IM", "intramuscular [fr:intramusculaire]", "IM");
    Route IO = new Route("IO", "intraosseous [fr:intraosseux]", "IO");
    Route OC = new Route("OC", "ocular [fr:oculaire]", "OC");

    // ==== BEGIN GENERATED OUTPUT ====
    // Produced by executing: get_meds -s 2 -e 3 -f 4 -E 7 -F 8 mml-buendia.xlsx

    Category ORAL = new Category("DORA", "oral", false, PO).withDrugs(
        new Drug("DORAABCV", "ABACAVIR sulfate (ABC) [fr:ABACAVIR sulfate (ABC)]").withFormats(
            new Format("DORAABCV3T", "eq. 300 mg base, tab. [fr:éq. 300 mg base, comp.]", Unit.TABLET),
            new Format("DORAABCV6TD", "60 mg, disp. tab. [fr:60 mg, comp. disp.]", Unit.TABLET)
        ),
        new Drug("DORAABLA", "ABC / 3TC [fr:ABC / 3TC]").withFormats(
            new Format("DORAABLA1TD", "60 mg / 30 mg, disp. tab. [fr:60 mg / 30 mg, comp. disp.]", Unit.TABLET),
            new Format("DORAABLA2T3", "600 mg / 300 mg, tab. [fr:600 mg / 300 mg, comp.]", Unit.TABLET),
            new Format("DORAABLA3TD", "120 mg / 60 mg, disp. tab. [fr:120 mg / 60 mg, comp. disp.]", Unit.TABLET)
        ),
        new Drug("DORAABLZ", "ABC / 3TC / AZT [fr:ABC / 3TC / AZT]").withFormats(
            new Format("DORAABLZ1T", "60 mg / 30 mg / 60 mg, tab. [fr:60 mg / 30 mg / 60 mg, comp.]", Unit.TABLET),
            new Format("DORAABLZ2T", "300 mg / 150 mg / 300 mg, tab. [fr:300 mg / 150 mg / 300 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAACEN", "ACENOCOUMAROL [fr:ACENOCOUMAROL]").withFormats(
            new Format("DORAACEN4T", "4 mg, tab. [fr:4 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAACET", "ACETAZOLAMIDE [fr:ACETAZOLAMIDE]").withFormats(
            new Format("DORAACET2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAACIV", "ACICLOVIR [fr:ACICLOVIR]").withFormats(
            new Format("DORAACIV2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORAACIV4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET),
            new Format("DORAACIV8T", "800 mg, tab. [fr:800 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAACSA", "ACETYLSALICYLIC acid (aspirin) [fr:Acide ACETYLSALICYLIQUE (aspirine)]").withFormats(
            new Format("DORAACSA3T", "300 mg, tab. [fr:300 mg, comp.]", Unit.TABLET),
            new Format("DORAACSA3TD", "300 mg, disp. tab. [fr:300 mg, comp. disp.]", Unit.TABLET),
            new Format("DORAACSA5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET),
            new Format("DORAACSA7TG", "75 mg, gastro-resistant tab. [fr:75 mg, comp. gastrorés.]", Unit.TABLET)
        ),
        new Drug("DORAALBE", "ALBENDAZOLE [fr:ALBENDAZOLE]").withFormats(
            new Format("DORAALBE1S", "200 mg / 5 ml, oral susp., 10 ml, bot. [fr:200 mg / 5 ml, susp. orale, 10 ml, fl.]", Unit.ML),
            new Format("DORAALBE2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORAALBE4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAALLO", "ALLOPURINOL [fr:ALLOPURINOL]").withFormats(
            new Format("DORAALLO1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAALUM", "ALUMINIUM hydroxide / MAGNESIUM hydroxide [fr:ALUMINIUM hydroxyde / MAGNESIUM hydroxyde]").withFormats(
            new Format("DORAALUM44TC", "400 mg / 400 mg, chew. tab. [fr:400 mg / 400 mg, cp. à mâcher]", Unit.TABLET)
        ),
        new Drug("DORAAMIO", "AMIODARONE hydrochloride [fr:AMIODARONE chlorhydrate]").withFormats(
            new Format("DORAAMIO2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAAMIT", "AMITRIPTYLINE hydrochloride [fr:AMITRIPTYLINE chlorhydrate]").withFormats(
            new Format("DORAAMIT2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAAMLO", "AMLODIPINE [fr:AMLODIPINE]").withFormats(
            new Format("DORAAMLO5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAAMOC", "AMOXICILLIN / CLAVULANIC acid [fr:AMOXICILLINE / acide CLAVULANIQUE]").withFormats(
            new Format("DORAAMOC1S6", "500 mg / 62.5 mg / 5 ml, powder oral susp 60 ml [fr:500 mg / CLAVULANIQUE62.5 mg / 5 ml, poudre susp. orale 60 ml]", Unit.ML),
            new Format("DORAAMOC22TD", "200 mg / 28.5 mg, disp. tab. [fr:200 mg / 28.5 mg, comp. disp.]", Unit.TABLET),
            new Format("DORAAMOC4S5", "400 mg / 57 mg / 5 ml, powd. oral susp. 70 ml [fr:400 mg / 57 mg / 5 ml, poudre susp. orale 70 ml]", Unit.ML),
            new Format("DORAAMOC56T", "500 mg / 62.5 mg, tab. [fr:500 mg / ac. 62.5 mg, comp.]", Unit.TABLET),
            new Format("DORAAMOC81T", "875 mg / 125 mg, tab. [fr:875 mg / ac. 125 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAAMOX", "AMOXICILLIN [fr:AMOXICILLINE]").withFormats(
            new Format("DORAAMOX1S1", "125 mg / 5 ml, powder oral susp., 100 ml, bot. [fr:125 mg / 5 ml, poudre susp. orale, 100 ml, fl]", Unit.ML),
            new Format("DORAAMOX1S6", "125 mg / 5 ml, powder oral susp., 60 ml, bot. [fr:125 mg / 5 ml, poudre susp. orale, 60 ml, fl]", Unit.ML),
            new Format("DORAAMOX2C", "250 mg, caps. [fr:250 mg, gél.]", Unit.CAPSULE),
            new Format("DORAAMOX2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORAAMOX2TDB", "250 mg, disp. and breakable tab. [fr:250 mg, comp. disp et sécable]", Unit.TABLET),
            new Format("DORAAMOX5C", "500 mg, caps. [fr:500 mg, gél.]", Unit.CAPSULE),
            new Format("DORAAMOX5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAARLU", "AL (ARTEMETHER / LUMEFANTRINE) [fr:AL (ARTEMETHER / LUMEFANTRINE)]").withFormats(
            new Format("DORAARLU1TD1", "20/120 mg, blister of 6 disp. tab., 5-14 kg [fr:20/120 mg, blister de 6 comp. disp., 5-14 kg]", Unit.TABLET),
            new Format("DORAARLU2TD1", "20/120 mg, blister of 12 disp. tab., 15-24 kg [fr:20/120 mg, blister de 12 comp. disp., 15-24 kg]", Unit.TABLET),
            new Format("DORAARLU3T1", "20/120 mg, blister of 18 tab., 25-34 kg [fr:20/120 mg, blister de 18 comp., 25-34 kg]", Unit.TABLET),
            new Format("DORAARLU4T1", "20/120 mg, blister of 24 tab., >35 kg [fr:20/120 mg, blister de 24 comp., >35 kg]", Unit.TABLET),
            new Format("DORAARLU5T1", "80/480 mg, blister of 6 tab., >35 kg [fr:80/480 mg, blister de 6 comp., >35 kg]", Unit.TABLET)
        ),
        new Drug("DORAASAQ", "AS / AQ (ARTESUNATE / AMODIAQUINE) [fr:AS / AQ (ARTESUNATE / AMODIAQUINE)]").withFormats(
            new Format("DORAASAQ1T1", "25 mg / eq. 67.5 mg base, blister of 3 tab, 4.5-8 kg [fr:25 mg / éq. 67.5 mg base, blister de 3 comp, 4.5-8 kg]", Unit.TABLET),
            new Format("DORAASAQ2T1", "50 mg / eq. 135 mg base, blister of 3 tab, 9-17 kg [fr:50 mg / éq. 135 mg base, blister de 3 comp., 9-17 kg]", Unit.TABLET),
            new Format("DORAASAQ3T1", "100 mg / eq. 270 mg base, blister of 3 tab, 18-35 kg [fr:100 mg / éq. 270 mg base, blister de 3 comp., 18-35kg]", Unit.TABLET),
            new Format("DORAASAQ4T1", "100 mg / eq. 270 mg base, blister of 6 tab., >36 kg [fr:100 mg / éq. 270 mg base, blister de 6 comp., >36 kg]", Unit.TABLET)
        ),
        new Drug("DORAASCA", "ASCORBIC acid (vitamin C) [fr:Acide ASCORBIQUE (vitamine C)]").withFormats(
            new Format("DORAASCA05T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET),
            new Format("DORAASCA2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORAASCA5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAASMQ", "AS / MQ (ARTESUNATE / MEFLOQUINE) [fr:AS / MQ (ARTESUNATE / MEFLOQUINE)]").withFormats(
            new Format("DORAASMQ1T1", "25 mg / eq. 50 mg base, blister of 3 tab., 5-8 kg [fr:25 mg / éq. 50 mg base, blister de 3 comp., 5-8 kg]", Unit.TABLET),
            new Format("DORAASMQ2T1", "25 mg / eq. 50 mg base, blister of 6 tab., 9-17 kg [fr:25 mg / éq. 50 mg base, blister of 6 comp., 9-17 kg]", Unit.TABLET),
            new Format("DORAASMQ3T1", "100 mg / eq. 200 mg base, blister of 3 tab., 18-29 kg [fr:100 mg / éq. 200 mg base, blister de 3 comp., 18-29 kg]", Unit.TABLET),
            new Format("DORAASMQ4T1", "100 mg / eq. 200 mg base, blister of 6 tab., >30 kg [fr:100 mg / éq. 200 mg base, blister de 6 comp., >30 kg]", Unit.TABLET)
        ),
        new Drug("DORAATAZ", "ATAZANAVIR sulfate (ATV) [fr:ATAZANAVIR sulfate (ATV)]").withFormats(
            new Format("DORAATAZ2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAATOP", "ATOVAQUONE / PROGUANIL HCl [fr:ATOVAQUONE / PROGUANIL HCl]").withFormats(
            new Format("DORAATOP1T1", "62.5 mg / 25 mg, tab., 11-40kg [fr:62.5 mg / 25 mg, comp, blister, 11-40kg]", Unit.TABLET),
            new Format("DORAATOP2T1", "250 mg / 100 mg, tab., >40 kg [fr:250 mg / 100 mg, comp, blister, >40 kg]", Unit.TABLET)
        ),
        new Drug("DORAATOR", "ATORVASTATIN calcium [fr:ATORVASTATINE calcique]").withFormats(
            new Format("DORAATOR1T", "eq. 10 mg base, tab. [fr:10 mg base, comp.]", Unit.TABLET),
            new Format("DORAATOR2T", "eq. 20 mg base, tab. [fr:eq. 20 mg base, comp.]", Unit.TABLET),
            new Format("DORAATOR4T", "eq. 40 mg base, tab. [fr:eq. 40 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAATVR", "ATV / r [fr:ATV / r]").withFormats(
            new Format("DORAATVR3T", "300 mg / 100 mg, tab. [fr:300 mg / 100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAAZIT", "AZITHROMYCIN [fr:AZITHROMYCINE]").withFormats(
            new Format("DORAAZIT2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORAAZIT3S", "200 mg / 5 ml, powder oral susp., 30 ml, bot. [fr:200 mg / 5 ml, poudre susp. orale, 30 ml, fl.]", Unit.ML),
            new Format("DORAAZIT5T", "500 mg, tab [fr:500 mg, comp]", Unit.TABLET)
        ),
        new Drug("DORABECL", "BECLOMETASONE dipropionate [fr:BECLOMETASONE dipropionate]").withFormats(
            new Format("DORABECL1SF", "0.10 mg / puff, 200 puffs, aerosol [fr:0.10 mg / bouffée, 200 b., aérosol]", Unit.PUFF),
            new Format("DORABECL2SF", "0.25 mg / puff, 200 puffs, aerosol [fr:0.25 mg / bouffée, 200 b., aérosol]", Unit.PUFF),
            new Format("DORABECL5SF", "0.05 mg / puff, 200 puffs, aerosol [fr:0.05 mg / bouffée, 200 b., aérosol]", Unit.PUFF)
        ),
        new Drug("DORABEDA", "BEDAQUILINE [fr:BEDAQUILINE]").withFormats(
            new Format("DORABEDA1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORABEND", "BENZNIDAZOLE [fr:BENZNIDAZOLE]").withFormats(
            new Format("DORABEND1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORABIPE", "BIPERIDEN hydrochloride [fr:BIPERIDENE chlorhydrate]").withFormats(
            new Format("DORABIPE2T", "2 mg, tab [fr:2 mg, comp]", Unit.TABLET)
        ),
        new Drug("DORABISA", "BISACODYL [fr:BISACODYL]").withFormats(
            new Format("DORABISA5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORABISO", "BISOPROLOL fumarate [fr:BISOPROLOL fumarate]").withFormats(
            new Format("DORABISO1TB4", "10 mg, break. tab. in 1/4 [fr:10 mg, comp. quadrisécable]", Unit.TABLET),
            new Format("DORABISO2TB", "2.5 mg, break. tab. [fr:2.5 mg, comp. séc.]", Unit.TABLET),
            new Format("DORABISO5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACABG", "CABERGOLINE [fr:CABERGOLINE]").withFormats(
            new Format("DORACABG5TB", "0.5 mg, break. tab. [fr:0.5 mg, comp. séc.]", Unit.TABLET)
        ),
        new Drug("DORACALC", "CALCIUM carbonate [fr:CALCIUM carbonate]").withFormats(
            new Format("DORACALC5TC", "eq. 500 mg Ca, chewable tab. [fr:éq. 500 mg Ca, comp. à mâcher]", Unit.TABLET),
            new Format("DORACALC6TC", "eq. 600 mg Ca, chewable tab. [fr:éq. 600 mg Ca, comp. à mâcher]", Unit.TABLET)
        ),
        new Drug("DORACALL", "CALCIUM lactate [fr:CALCIUM lactate]").withFormats(
            new Format("DORACALL3T", "300 mg, eq. to 39 mg Ca, tab. [fr:300 mg, éq. à 39 mg Ca, comp.]", Unit.TABLET)
        ),
        new Drug("DORACARB", "CARBAMAZEPINE [fr:CARBAMAZEPINE]").withFormats(
            new Format("DORACARB2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACARC", "CARBOCISTEINE [fr:CARBOCISTEINE]").withFormats(
            new Format("DORACARC1S", "250 mg / 5 ml, oral sol., 200 ml, bot. [fr:250 mg / 5 ml, sol. orale, 200 ml, fl.]", Unit.ML)
        ),
        new Drug("DORACARV", "CARVEDILOL [fr:CAREVEDILOL]").withFormats(
            new Format("DORACARV3TB", "3.125 mg, breakable tab. [fr:3.125 mg, comp. sécable]", Unit.TABLET),
            new Format("DORACARV6TB", "6.25 mg, breakable tab. [fr:6.25 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORACARZ", "CARBIMAZOLE [fr:CARBIMAZOLE]").withFormats(
            new Format("DORACARZ2T", "20 mg, tab. [fr:20 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACEFI", "CEFIXIME [fr:CEFIXIME]").withFormats(
            new Format("DORACEFI1S", "100 mg / 5 ml, powder for oral susp., 40 ml, bot. [fr:100 mg / 5 ml, poudre pour susp. orale, 40 ml, fl.]", Unit.ML),
            new Format("DORACEFI2S", "100 mg / 5 ml, powder for oral susp., 60 ml, bot. [fr:100 mg / 5 ml, poudre pour susp. orale, 60 ml, fl.]", Unit.ML),
            new Format("DORACEFI2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORACEFI4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACEFX", "CEFALEXIN [fr:CEFALEXINE]").withFormats(
            new Format("DORACEFX1S", "125 mg / 5 ml, granules oral susp., 100 ml, bot. [fr:125 mg / 5 ml, granules susp. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORACEFX2C", "250 mg, caps. [fr:250 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORACETI", "CETIRIZINE [fr:CETIRIZINE]").withFormats(
            new Format("DORACETI1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACHAR", "CHARCOAL ACTIVATED [fr:CHARBON ACTIVE]").withFormats(
            new Format("DORACHAR5G", "granules for oral susp., 50 g, bot. [fr:granules pour susp. orale, 50 g, fl.]", Unit.MG)
        ),
        new Drug("DORACHLM", "CHLORPROMAZINE hydrochloride [fr:CHLORPROMAZINE chlorhydrate]").withFormats(
            new Format("DORACHLM1T", "eq. 100 mg base, tab. [fr:éq. 100 mg base, comp.]", Unit.TABLET),
            new Format("DORACHLM2T", "eq. 25 mg base, tab. [fr:éq. 25 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORACHLO", "CHLORAMPHENICOL [fr:CHLORAMPHENICOL]").withFormats(
            new Format("DORACHLO2C", "250 mg, caps. [fr:250 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORACHLQ", "CHLOROQUINE [fr:CHLOROQUINE]").withFormats(
            new Format("DORACHLQ2S1", "eq. 25 mg base / 5 ml, syrup, 150 ml, bot. [fr:éq. 25 mg base / 5 ml, sirop, 150 ml, fl.]", Unit.ML),
            new Format("DORACHLQ3T", "155 mg base, (250 mg phosphate), tab. [fr:155 mg base, (250 mg phosphate), comp.]", Unit.TABLET)
        ),
        new Drug("DORACIME", "CIMETIDINE [fr:CIMETIDINE]").withFormats(
            new Format("DORACIME2TE", "200 mg, effervescent tab. [fr:200 mg, comp. effervescent]", Unit.TABLET)
        ),
        new Drug("DORACIPR", "CIPROFLOXACIN [fr:CIPROFLOXACINE]").withFormats(
            new Format("DORACIPR1S", "250 mg / 5 ml, gran. + solvent oral susp [fr:250 mg / 5 ml, gran. + solvant susp. orale]", Unit.ML),
            new Format("DORACIPR2T", "hydrochloride, eq. 250 mg base, tab. [fr:chlorhydrate, éq. 250 mg base, comp.]", Unit.TABLET),
            new Format("DORACIPR5T", "hydrochloride, eq. 500 mg base, tab. [fr:chlorhydrate, éq. 500 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORACLAR", "CLARITHROMYCIN [fr:CLARITHROMYCINE]").withFormats(
            new Format("DORACLAR1S", "250 mg / 5 ml, granules for oral susp., bot. [fr:250 mg / 5 ml, granules susp. buv., fl.]", Unit.ML),
            new Format("DORACLAR2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORACLAR5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACLIN", "CLINDAMYCIN hydrochloride [fr:CLINDAMYCINE chlorhydrate]").withFormats(
            new Format("DORACLIN1C", "eq. 150 mg base, caps. [fr:éq. 150 mg base, gél.]", Unit.CAPSULE),
            new Format("DORACLIN3C", "eq. 300 mg base, caps. [fr:éq. 300 mg base, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORACLOF", "CLOFAZIMINE [fr:CLOFAZIMINE]").withFormats(
            new Format("DORACLOF1C", "100 mg, soft caps. [fr:100 mg, caps. molle]", Unit.CAPSULE),
            new Format("DORACLOF1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET),
            new Format("DORACLOF5C", "50 mg, soft caps. [fr:50 mg, caps. molle]", Unit.CAPSULE),
            new Format("DORACLOF5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACLOP", "CLOPIDOGREL [fr:CLOPIDOGREL]").withFormats(
            new Format("DORACLOP7T", "75 mg, tab. [fr:75 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACLOX", "CLOXACILLIN sodium [fr:CLOXACILLINE sodique]").withFormats(
            new Format("DORACLOX2C", "eq. 250 mg base, caps. [fr:éq. 250 mg base, gél.]", Unit.CAPSULE),
            new Format("DORACLOX5C", "eq. 500 mg base, caps. [fr:éq. 500 mg base, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORACODE", "CODEINE phosphate [fr:CODEINE phosphate]").withFormats(
            new Format("DORACODE1S", "15 mg / 5 ml, syrup, 200 ml, bot. [fr:15 mg / 5 ml, sirop, 200 ml, fl.]", Unit.ML),
            new Format("DORACODE3T", "30 mg, tab. [fr:30 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACOLC", "COLECALCIFEROL (vit. D3) [fr:COLECALCIFEROL (vit. D3)]").withFormats(
            new Format("DORACOLC1S1", "10.000 IU / ml, sol., 10 ml, bot. [fr:10000 UI / ml, sol., 10 ml, fl.]", Unit.ML)
        ),
        new Drug("DORACOLH", "COLCHICINE [fr:COLCHICINE]").withFormats(
            new Format("DORACOLH1T", "1 mg, tab. [fr:1 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACOTR", "COTRIMOXAZOLE [fr:COTRIMOXAZOLE]").withFormats(
            new Format("DORACOTR1TD", "100 mg / 20 mg, disp. tab. [fr:100 mg / 20 mg, comp. disp.]", Unit.TABLET),
            new Format("DORACOTR2S1", "200 mg / 40 mg / 5 ml, oral susp, 100 ml, bot. [fr:200 mg / 40 mg / 5 ml, susp orale, 100 ml, fl.]", Unit.ML),
            new Format("DORACOTR4T", "400 mg / 80 mg, tab. [fr:400 mg / 80 mg, comp.]", Unit.TABLET),
            new Format("DORACOTR8T", "800 mg / 160 mg, tab. [fr:800 mg / 160 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORACYCL", "CYCLOSERINE [fr:CYCLOSERINE]").withFormats(
            new Format("DORACYCL1C1", "125 mg, caps. blister [fr:125 mg, gél. blister]", Unit.CAPSULE),
            new Format("DORACYCL2C1", "250 mg, caps. blister [fr:250 mg, gél. blister]", Unit.CAPSULE),
            new Format("DORACYCL2C3", "250 mg, caps. bulk [fr:250 mg, gél. vrac]", Unit.CAPSULE)
        ),
        new Drug("DORACYCS", "CYCLIZINE [fr:CYCLIZINE]").withFormats(
            new Format("DORACYCS5T", "50 mg, tabs. [fr:50 mg, tabs.]", Unit.MG)
        ),
        new Drug("DORADACL", "DACLATASVIR dihydrochloride (DCV) [fr:DACLATASVIR dichlorhydrate (DCV)]").withFormats(
            new Format("DORADACL3T", "eq. 30 mg base, tab. [fr:éq. 30 mg base, comp.]", Unit.TABLET),
            new Format("DORADACL6T", "eq. 60 mg base, tab. [fr:éq. 60 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORADAPS", "DAPSONE [fr:DAPSONE]").withFormats(
            new Format("DORADAPS1TB", "100 mg, break. tab. [fr:100 mg, comp. sécable]", Unit.TABLET),
            new Format("DORADAPS2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET),
            new Format("DORADAPS5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADARU", "DARUNAVIR ethanolate (DRV) [fr:DARUNAVIR éthanolate (DRV)]").withFormats(
            new Format("DORADARU1T", "eq. 150 mg base, tab. [fr:éq. 150 mg base, comp.]", Unit.TABLET),
            new Format("DORADARU3T", "eq. 300 mg base, tab. [fr:éq. 300 mg base, comp.]", Unit.TABLET),
            new Format("DORADARU4T", "eq. 400 mg base, tab. [fr:éq. 400 mg base, comp.]", Unit.TABLET),
            new Format("DORADARU6T", "eq. 600 mg base, tab. [fr:éq. 600 mg base, comp.]", Unit.TABLET),
            new Format("DORADARU7T", "eq. 75 mg base, tab. [fr:éq. 75 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORADEFP", "DEFERIPRONE [fr:DEFERIPRONE]").withFormats(
            new Format("DORADEFP2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORADEFP5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADEFS", "DEFERASIROX [fr:DEFERASIROX]").withFormats(
            new Format("DORADEFS1TD", "125 mg, disp. tab. [fr:125 mg, comp. disp.]", Unit.TABLET),
            new Format("DORADEFS2TD", "250 mg, disp. tab. [fr:250 mg, comp. disp.]", Unit.TABLET),
            new Format("DORADEFS5TD", "500 mg, disp. tab. [fr:500 mg, comp. disp.]", Unit.TABLET)
        ),
        new Drug("DORADELA", "DELAMANID [fr:DELAMANID]").withFormats(
            new Format("DORADELA5T1", "50 mg, tab., blister [fr:50 mg, comp., blister]", Unit.TABLET)
        ),
        new Drug("DORADESO", "DESOGESTREL [fr:DESOGESTREL]").withFormats(
            new Format("DORADESO7T1", "0.075 mg, blister of 28 tab. [fr:0.075 mg, blister de 28 comp.]", Unit.TABLET)
        ),
        new Drug("DORADHAP", "DHA / PPQ (DIHYDROARTEMISININ / PIPERAQUINE) [fr:DHA / PPQ (DIHYDROARTEMISININ / PIPERAQUINE)]").withFormats(
            new Format("DORADHAP1T1", "20 mg / 160 mg, blister de 3 tab., 5-12 kg [fr:20 mg / 160 mg, blister de 3 comp., 5-12 kg]", Unit.TABLET),
            new Format("DORADHAP2T1", "40 mg / 320 mg, blister of 3 tab., 13-23 kg [fr:40 mg / 320 mg, blister de 3 comp., 13-23 kg]", Unit.TABLET),
            new Format("DORADHAP3T1", "40 mg / 320 mg, blister of 6 tab., 24-34 kg [fr:40 mg / 320 mg, blister de 6 comp., 24-34 kg]", Unit.TABLET),
            new Format("DORADHAP4T1", "40 mg / 320 mg, blister of 9 tab., 35-74 kg [fr:40 mg / 320 mg, blister de 9 comp., 35-74 kg]", Unit.TABLET),
            new Format("DORADHAP5T1", "40 mg / 320 mg, blister de 12 tab., 75-100 kg [fr:40 mg / 320 mg, blister de 12 comp., 75-100 kg]", Unit.TABLET)
        ),
        new Drug("DORADIAC", "DIACETYLCYSTEINATE methyle [fr:DIACETYLCYSTEINE méthyl]").withFormats(
            new Format("DORADIAC2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADIAZ", "DIAZEPAM [fr:DIAZEPAM]").withFormats(
            new Format("DORADIAZ2T", "2 mg, tab. [fr:2 mg, comp.]", Unit.TABLET),
            new Format("DORADIAZ5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADICL", "DICLOFENAC sodium [fr:DICLOFENAC sodique]").withFormats(
            new Format("DORADICL2TG", "25 mg, gastro-resistant tab. [fr:25 mg, comp. gastro-résistant]", Unit.TABLET)
        ),
        new Drug("DORADIET", "DIETHYLCARBAMAZINE citrate [fr:DIETHYLCARBAMAZINE citrate]").withFormats(
            new Format("DORADIET1TB", "eq. 100 mg base, break. tab. [fr:éq. 100 mg base, comp. séc.]", Unit.TABLET)
        ),
        new Drug("DORADIGO", "DIGOXIN [fr:DIGOXINE]").withFormats(
            new Format("DORADIGO2T", "0.25 mg, tab. [fr:0.25 mg, comp.]", Unit.TABLET),
            new Format("DORADIGO6T", "0.0625 mg, tab. [fr:0.0625 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADIPH", "DIPHENHYDRAMINE [fr:DIPHENHYDRAMINE]").withFormats(
            new Format("DORADIPH9T", "90 mg, tab. [fr:90 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORADOLU", "DOLUTEGRAVIR sodium (DTG) [fr:DOLUTEGRAVIR sodium (DTG)]").withFormats(
            new Format("DORADOLU5T", "eq. 50 mg base, tab. [fr:éq. 50 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORADOXY", "DOXYCYCLINE salt [fr:DOXYCYCLINE sel]").withFormats(
            new Format("DORADOXY1T", "eq. 100 mg base, tab. [fr:éq. 100 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAEFAV", "EFAVIRENZ (EFV) [fr:EFAVIRENZ (EFV)]").withFormats(
            new Format("DORAEFAV2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE),
            new Format("DORAEFAV2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORAEFAV2TB", "200 mg, break. tab. [fr:200 mg, comp. séc.]", Unit.TABLET),
            new Format("DORAEFAV6T", "600 mg, tab. [fr:600 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAEHRI", "E / H / R (ETHAMBUTOL / ISONIAZID / RIFAMPICIN) [fr:E / H / R (ETHAMBUTOL / ISONIAZID / RIFAMPICIN)]").withFormats(
            new Format("DORAEHRI1T1", "275 mg / 75 mg / 150 mg, tab., blister [fr:275 mg / 75 mg / 150 mg, comp., blister]", Unit.TABLET),
            new Format("DORAEHRI1T3", "275 mg / 75 mg / 150 mg, tab., bulk [fr:275 mg / 75 mg / 150 mg, comp., vrac]", Unit.TABLET)
        ),
        new Drug("DORAEHZR", "E / H / Z / R (ETHAMBUTOL / ISONIAZID / PYRAZINAMIDE / RIFAMPICIN) [fr:E / H / Z / R (ETHAMBUTOL / ISONIAZID / PYRAZINAMIDE / RIFAMPICIN)]").withFormats(
            new Format("DORAEHZR2T1", "275 mg / 75 mg / 400 mg / 150 mg, tab., blister [fr:275 mg / 75 mg / 400 mg / 150 mg, comp., blister]", Unit.TABLET),
            new Format("DORAEHZR2T3", "275 mg / 75 mg / 400 mg / 150 mg, tab., bulk [fr:275 mg / 75 mg / 400 mg / 150 mg, comp., vrac]", Unit.TABLET)
        ),
        new Drug("DORAENAL", "ENALAPRIL maleate [fr:ENALAPRIL maléate]").withFormats(
            new Format("DORAENAL2T", "20 mg, tab. [fr:20 mg, comp.]", Unit.TABLET),
            new Format("DORAENAL5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAERYT", "ERYTHROMYCIN [fr:ERYTHROMYCINE]").withFormats(
            new Format("DORAERYT1G", "ethylsucc. 125 mg, gran. for oral susp., sachet [fr:ethylsucc, 125 mg, gran. pour susp. orale, sachet]", Unit.MG),
            new Format("DORAERYT1S1", "ethylsucc. 125 mg / 5 ml, powder oral susp. 100 ml, bot [fr:ethylsucc, 125 mg / 5 ml, poudre susp. orale, 100 ml, fl]", Unit.ML),
            new Format("DORAERYT2T", "stearate, eq. 250 mg base, tab. [fr:stéarate, eq. 250 mg base, comp.]", Unit.TABLET),
            new Format("DORAERYT5T", "stearate, eq. 500 mg base, tab. [fr:stéarate, eq. 500 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAETHA", "ETHAMBUTOL hydrochloride (E) [fr:ETHAMBUTOL chlorhydrate (E)]").withFormats(
            new Format("DORAETHA1T1", "eq. 100 mg base, tab. blister [fr:éq. 100 mg base, comp. blister]", Unit.TABLET),
            new Format("DORAETHA1T3", "eq. 100 mg base, tab. bulk [fr:éq. 100 mg base, comp. vrac]", Unit.TABLET),
            new Format("DORAETHA4T1", "eq. 400 mg base, tab. blister [fr:éq. 400 mg base, comp. blister]", Unit.TABLET),
            new Format("DORAETHA4T3", "eq. 400 mg base, tab. bulk [fr:éq. 400 mg base, comp. vrac]", Unit.TABLET)
        ),
        new Drug("DORAETHL", "ETHINYLESTRADIOL / LEVONORGESTREL [fr:ETHINYLESTRADIOL / LEVONORGESTREL]").withFormats(
            new Format("DORAETHL31T", "0.03 mg / 0.15 mg, blister 28tab [fr:0.03 mg / 0.15 mg, plaq. 28 comp.]", Unit.MG)
        ),
        new Drug("DORAETHN", "ETHIONAMIDE [fr:ETHIONAMIDE]").withFormats(
            new Format("DORAETHN1T1", "125 mg, tab., blister [fr:125 mg, comp., blister]", Unit.TABLET),
            new Format("DORAETHN1TD1", "125 mg, dispersible tab., blister [fr:125 mg, comp. dispersible, blister]", Unit.TABLET),
            new Format("DORAETHN2T1", "250 mg, tab., blister [fr:250 mg, comp., blister]", Unit.TABLET)
        ),
        new Drug("DORAETRA", "ETRAVIRINE (ETV) [fr:ETRAVIRINE (ETV)]").withFormats(
            new Format("DORAETRA1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAFENO", "FENOFIBRATE [fr:FENOFIBRATE]").withFormats(
            new Format("DORAFENO2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAFERF", "FERROUS salt / FOLIC acid (FEFOL) [fr:sel de FER / acide FOLIQUE (FEFOL)]").withFormats(
            new Format("DORAFERF14T", "eq. 60 mg iron / 0.4 mg, tab [fr:éq. 60 mg fer / 0.4 mg, comp.]", Unit.TABLET),
            new Format("DORAFERF24T", "eq. 65 mg iron / 0.4 mg, tab [fr:éq. 65 mg fer / 0.4 mg, comp.]", Unit.TABLET),
            new Format("DORAFERF45CP", "eq. 47 mg iron / 0.5 mg, prol. rel. caps. [fr:éq. 47 mg fer / 0.5 mg, gél. lib. prol.]", Unit.CAPSULE)
        ),
        new Drug("DORAFERS", "FERROUS salt [fr:sel de FER]").withFormats(
            new Format("DORAFERS2S", "eq. iron 45 mg / 5 ml, syrup, 200 ml, bot. [fr:éq. 45 mg / 5 ml fer, sirop, 200 ml, fl.]", Unit.ML),
            new Format("DORAFERS2T", "eq. + / - 65 mg iron, tab. [fr:éq. + / - 65 mg de fer, comp.]", Unit.TABLET),
            new Format("DORAFERS3S", "eq. iron 45 mg / 5 ml, syrup, 300 ml, bot. [fr:éq. 45 mg / 5 ml fer, sirop, 300 ml, fl.]", Unit.ML),
            new Format("DORAFERS4S", "sodium FEREDETATE, eq. 34 mg / 5 ml iron, 125 ml, bot. [fr:FEREDETATE sodium, eq. 34 mg / 5 ml fer, 125 ml, fl.]", Unit.ML)
        ),
        new Drug("DORAFEXI", "FEXINIDAZOLE [fr:FEXINIDAZOLE]").withFormats(
            new Format("DORAFEXI6T1A", "600 mg, wallet of 24 tabs., >34 kg [fr:600 mg, wallet of 24 tabs., >34 kg]", Unit.MG),
            new Format("DORAFEXI6T1P", "600 mg, wallet of 14 tabs., 20-34 kg [fr:600 mg, wallet of 14 tabs., 20-34 kg]", Unit.MG)
        ),
        new Drug("DORAFLUC", "FLUCONAZOLE [fr:FLUCONAZOLE]").withFormats(
            new Format("DORAFLUC1C", "100 mg, caps. [fr:100 mg, gél.]", Unit.CAPSULE),
            new Format("DORAFLUC1S", "50 mg / 5 ml, powder oral susp., bot. [fr:50 mg / 5 ml, poudre susp. orale, fl.]", Unit.ML),
            new Format("DORAFLUC2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE),
            new Format("DORAFLUC2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORAFLUC5C", "50 mg, caps. [fr:50 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAFLUS", "FLUTICASONE / SALMETEROL [fr:FLUTICASONE / SALMETEROL]").withFormats(
            new Format("DORAFLUS12SF", "125 µg / 25 µg / puff, aerosol [fr:125 µg / 25 µg / bouffée, aérosol]", Unit.PUFF)
        ),
        new Drug("DORAFLUT", "FLUTICASONE propionate [fr:FLUTICASONE propionate]").withFormats(
            new Format("DORAFLUT5SF", "50 µg / puff, aerosol, 120 doses [fr:50 µg / bouffée, aérosol, 120 doses]", Unit.PUFF)
        ),
        new Drug("DORAFLUX", "FLUOXETINE hydrochloride [fr:FLUOXETINE chlorhydrate]").withFormats(
            new Format("DORAFLUX2C", "eq. 20 mg base, caps. [fr:éq. 20 mg base, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAFLUY", "FLUCYTOSINE [fr:FLUCYTOSINE]").withFormats(
            new Format("DORAFLUY5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAFOLA", "FOLIC acid [fr:Acide FOLIQUE]").withFormats(
            new Format("DORAFOLA5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAFOLC", "CALCIUM FOLINATE [fr:FOLINATE de CALCIUM]").withFormats(
            new Format("DORAFOLC1T", "eq. 15 mg, folinic acid, tab. [fr:éq. 15 mg, acide folinique, comp.]", Unit.TABLET),
            new Format("DORAFOLC2C", "eq. 25 mg folinic acid, caps. [fr:éq. 25 mg acide folinique, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAFOSF", "FOSFOMYCIN trometamol [fr:FOSFOMYCINE trométamol]").withFormats(
            new Format("DORAFOSF3S", "eq. 3 g base, sachet [fr:éq. 3 g base, sachet]", Unit.MG)
        ),
        new Drug("DORAFURO", "FUROSEMIDE [fr:FUROSEMIDE]").withFormats(
            new Format("DORAFURO2T", "20 mg, tab. [fr:20 mg, comp.]", Unit.TABLET),
            new Format("DORAFURO4T", "40 mg, tab. [fr:40 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAGABA", "GABAPENTIN [fr:GABAPENTINE]").withFormats(
            new Format("DORAGABA1C", "100 mg caps. [fr:100 mg gél.]", Unit.CAPSULE),
            new Format("DORAGABA3C", "300 mg caps. [fr:300 mg gél.]", Unit.CAPSULE),
            new Format("DORAGABA4C", "400 mg, caps. [fr:400 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAGLIB", "GLIBENCLAMIDE [fr:GLIBENCLAMIDE]").withFormats(
            new Format("DORAGLIB5TB", "5 mg, breakable tab. [fr:5 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORAGLIC", "GLICLAZIDE [fr:GLICLAZIDE]").withFormats(
            new Format("DORAGLIC8TB", "80 mg, breakable tab. [fr:80 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORAGLIM", "GLIBENCLAMIDE / METFORMIN [fr:GLIBENCLAMIDE / METFORMINE]").withFormats(
            new Format("DORAGLIM55T", "5 mg / hydrochloride, 500 mg, tab. [fr:5 mg / chlorhydrate, 500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAGLYT", "GLYCERYL TRINITRATE [fr:GLYCERYLE TRINITRATE]").withFormats(
            new Format("DORAGLYT5T", "0.5 mg, sublingual tab. [fr:0.5 mg, comp. sublingual]", Unit.TABLET)
        ),
        new Drug("DORAGRIS", "GRISEOFULVIN [fr:GRISEOFULVINE]").withFormats(
            new Format("DORAGRIS1T", "125 mg, tab. [fr:125 mg, comp.]", Unit.TABLET),
            new Format("DORAGRIS5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHALO", "HALOTHANE [fr:HALOTHANE]").withFormats(
            new Format("DORAHALO1A2", "250 ml, bot. [fr:250 ml, fl.]", Unit.ML)
        ),
        new Drug("DORAHALP", "HALOPERIDOL [fr:HALOPERIDOL]").withFormats(
            new Format("DORAHALP05C", "0.5 mg, caps. [fr:0.5 mg, gél.]", Unit.CAPSULE),
            new Format("DORAHALP05T", "0.5 mg, tab. [fr:0.5 mg, comp.]", Unit.TABLET),
            new Format("DORAHALP1S2", "2 mg / ml, oral sol., 100 ml, bot. with pipette [fr:2 mg / ml, sol. orale, 100 ml, fl. avec pipette]", Unit.ML),
            new Format("DORAHALP1T", "1 mg, tab. [fr:1 mg, comp.]", Unit.TABLET),
            new Format("DORAHALP3D", "2 mg / ml / 20 drops, 30 ml, bot. [fr:2 mg / ml / 20 gouttes, 30 ml, fl.]", Unit.ML),
            new Format("DORAHALP5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHPST", "INH / PYRIDOXINE / SMX / TMP [fr:INH / PYRIDOXINE / SMX / TMP]").withFormats(
            new Format("DORAHPST32T", "300 mg / 25 mg / 800 mg / 160 mg, tab. [fr:300 mg / 25 mg / 800 mg / 160 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHRIF", "H / R (ISONIAZID / RIFAMPICIN) [fr:H / R (ISONIAZID / RIFAMPICIN)]").withFormats(
            new Format("DORAHRIF5TD1", "50 mg / 75 mg, disp. tab., blister [fr:50 mg / 75 mg, comp. disp., blister]", Unit.TABLET),
            new Format("DORAHRIF6TD1", "60 mg / 60 mg, disp. tab., blister [fr:60 mg / 60 mg, comp. disp., blister]", Unit.TABLET),
            new Format("DORAHRIF6TD3", "60 mg / 60 mg, disp. tab., bulk [fr:60 mg / 60 mg, comp. disp., vrac]", Unit.TABLET),
            new Format("DORAHRIF7T1", "75 mg / 150 mg, tab., blister [fr:75 mg / 150 mg, comp., blister]", Unit.TABLET),
            new Format("DORAHRIF7T3", "75 mg / 150 mg, tab., bulk [fr:75 mg / 150 mg, comp., vrac]", Unit.TABLET)
        ),
        new Drug("DORAHYDC", "HYDROXYCARBAMIDE [fr:HYDROXYCARBAMIDE]").withFormats(
            new Format("DORAHYDC1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHYDO", "HYDROCHLOROTHIAZIDE [fr:HYDROCHLOROTHIAZIDE]").withFormats(
            new Format("DORAHYDO1T", "12.5 mg, tab. [fr:12.5 mg, comp.]", Unit.TABLET),
            new Format("DORAHYDO2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHYDX", "HYDROXYZINE dihydrochloride [fr:HYDROXYZINE dichlorhydrate]").withFormats(
            new Format("DORAHYDX2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAHYOS", "HYOSCINE BUTYLBROMIDE (scopolamine butylbromide) [fr:BUTYLBROMURE HYOSCINE (butylbromure scopolamine)]").withFormats(
            new Format("DORAHYOS1T", "10 mg, tab [fr:10 mg, cp]", Unit.TABLET)
        ),
        new Drug("DORAHZRI", "H / Z / R (ISONIAZID / PYRAZINAMIDE / RIFAMPICIN) [fr:H / Z / R (ISONIAZID / PYRAZINAMIDE / RIFAMPICIN)]").withFormats(
            new Format("DORAHZRI5TD1", "50 mg / 150 mg / 75 mg, disp. tab., blister [fr:50 mg / 150 mg / 75 mg, comp. disp., blister]", Unit.TABLET)
        ),
        new Drug("DORAIBUP", "IBUPROFEN [fr:IBUPROFENE]").withFormats(
            new Format("DORAIBUP2S", "100 mg / 5 ml, oral susp., 150 ml, bot. [fr:100 mg / 5 ml, susp. orale, 150 ml, fl.]", Unit.ML),
            new Format("DORAIBUP2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORAIBUP3S", "100 mg / 5 ml, oral susp., 200 ml, bot. [fr:100 mg / 5 ml, susp. orale, 200 ml, fl.]", Unit.ML),
            new Format("DORAIBUP4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAIODO", "IODIZED OIL [fr:HUILE IODEE]").withFormats(
            new Format("DORAIODO1C", "190 mg, caps. [fr:190 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAIPRA", "IPRATROPIUM bromide [fr:IPRATROPIUM bromure]").withFormats(
            new Format("DORAIPRA2N", "0.250 mg / ml, 1 ml, sol. for nebuliser [fr:0.250 mg / ml, 1 ml, sol. pour nébuliseur]", Unit.ML),
            new Format("DORAIPRA2N2", "0.125 mg / ml, 2 ml, sol. for nebuliser [fr:0.125 mg / ml, 2 ml, sol. pour nébuliseur]", Unit.ML),
            new Format("DORAIPRA2SF", "20 µg / puff, 200 puffs, aerosol [fr:20 µg / bouffée, 200 bouffées, aerosol]", Unit.PUFF),
            new Format("DORAIPRA5N", "0.250 mg / ml, 2 ml, sol. for nebuliser [fr:0.250 mg / ml, 2 ml, sol. pour nébuliseur]", Unit.ML)
        ),
        new Drug("DORAISOB", "ISOSORBIDE DINITRATE [fr:ISOSORBIDE DINITRATE]").withFormats(
            new Format("DORAISOB1T", "10 mg, tab [fr:10 mg, comp]", Unit.TABLET),
            new Format("DORAISOB5T", "5 mg, sublingual tab. [fr:5 mg, comp. sublingual]", Unit.TABLET)
        ),
        new Drug("DORAISOF", "ISOFLURANE [fr:ISOFLURANE]").withFormats(
            new Format("DORAISOF2L", "liquid, 250 ml, bot. [fr:liquide, 250 ml, fl.]", Unit.ML)
        ),
        new Drug("DORAISON", "ISONIAZID (H) [fr:ISONIAZIDE (H)]").withFormats(
            new Format("DORAISON1T1", "100 mg, breakable tab., blister [fr:100 mg, comp. sécable, blister]", Unit.TABLET),
            new Format("DORAISON1T3", "100 mg, breakable tab., bulk [fr:100 mg, comp. sécable, vrac]", Unit.TABLET),
            new Format("DORAISON3T1", "300 mg, tab., blister [fr:300 mg, comp., blister]", Unit.TABLET),
            new Format("DORAISON3T3", "300 mg, tab., bulk [fr:300 mg, comp., vrac]", Unit.TABLET),
            new Format("DORAISON5S", "50 mg / 5 ml, oral sol., 500 ml, bot. [fr:50 mg / 5 ml, sol. orale, 500 ml, fl.]", Unit.ML)
        ),
        new Drug("DORAISOS", "ISOSORBIDE DINITRATE (prolonged) [fr:ISOSORBIDE DINITRATE (prolongé)]").withFormats(
            new Format("DORAISOS2T", "20 mg, prol. release, tab. [fr:20 mg, libér. prol., comp.]", Unit.TABLET)
        ),
        new Drug("DORAITRA", "ITRACONAZOLE [fr:ITRACONAZOLE]").withFormats(
            new Format("DORAITRA1C", "100 mg, caps. [fr:100 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAIVER", "IVERMECTIN [fr:IVERMECTINE]").withFormats(
            new Format("DORAIVER3T", "(onchocerciasis, mass distribution), 3 mg, tab. [fr:(onchocercose, distribution de masse), 3 mg, comp]", Unit.TABLET),
            new Format("DORAIVER3T4", "(onchocerciasis), 3 mg, tab. [fr:(onchocercose), 3 mg, comp.]", Unit.TABLET),
            new Format("DORAIVER3TS", "(scabies + other indic. ), 3 mg, tab. [fr:(gale + autres indic. ), 3 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALABE", "LABETALOL hydrochloride [fr:LABETALOL chlorhydrate]").withFormats(
            new Format("DORALABE1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET),
            new Format("DORALABE2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALACT", "LACTULOSE [fr:LACTULOSE]").withFormats(
            new Format("DORALACT1S", "min. 3.1 g / 5 ml, oral sol., bot. [fr:min. 3.1 g / 5 ml, sol. orale, fl.]", Unit.ML),
            new Format("DORALACT3S", "10 g / 15 ml, oral sol., sachet [fr:10 g / 15 ml, sol. orale, sachet]", Unit.ML)
        ),
        new Drug("DORALAMI", "LAMIVUDINE (3TC) [fr:LAMIVUDINE (3TC)]").withFormats(
            new Format("DORALAMI1S", "50 mg / 5 ml, oral sol., 100 ml, bot. [fr:50 mg / 5 ml, sol. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORALAMI1T", "150 mg, tab. [fr:150 mg, comp.]", Unit.TABLET),
            new Format("DORALAMI2S", "50 mg / 5 ml, oral sol., 240 ml, bot. [fr:50 mg / 5 ml, sol. orale, 240 ml, fl.]", Unit.ML)
        ),
        new Drug("DORALEFX", "LEVOFLOXACIN [fr:LEVOFLOXACINE]").withFormats(
            new Format("DORALEFX1TD1", "100 mg, dispersible tab., blister [fr:100 mg, comp. dispersible, blister]", Unit.TABLET),
            new Format("DORALEFX2T", "250 mg, tab., blister [fr:250 mg, comp., blister]", Unit.TABLET),
            new Format("DORALEFX5T", "500 mg, tab., blister [fr:500 mg, comp., blister]", Unit.TABLET)
        ),
        new Drug("DORALESO", "LEDIPASVIR [fr:LEDIPASVIR]").withFormats(
            new Format("DORALESO94T", "90 mg / SOFOSBUVIR 400 mg, tab. [fr:90 mg / SOFOSBUVIR 400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALEVC", "LEVODOPA [fr:LEVODOPA]").withFormats(
            new Format("DORALEVC2T", "250 mg / CARBIDOPA 25 mg, tab. [fr:250 mg / CARBIDOPA 25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALEVE", "LEVETIRACETAM [fr:LEVETIRACETAM]").withFormats(
            new Format("DORALEVE1S3", "500 mg / 5 ml, oral sol., 300 ml bot. [fr:500 mg / 5 ml, sol. orale, 300 ml fl.]", Unit.ML)
        ),
        new Drug("DORALEVN", "LEVONORGESTREL [fr:LEVONORGESTREL]").withFormats(
            new Format("DORALEVN1T", "1.5 mg, tab. [fr:1.5 mg, comp.]", Unit.TABLET),
            new Format("DORALEVN3T1", "0.03 mg, blister of 35 tab. [fr:0.03 mg, blister de 35 comp.]", Unit.TABLET)
        ),
        new Drug("DORALEVO", "LEVOTHYROXINE sodium [fr:LEVOTHYROXINE sodique]").withFormats(
            new Format("DORALEVO1T", "0.1 mg, tab. [fr:0.1 mg, cp]", Unit.TABLET),
            new Format("DORALEVO2T", "0.025 mg, tab. [fr:0.025 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALINE", "LINEZOLID [fr:LINEZOLIDE]").withFormats(
            new Format("DORALINE1S", "100 mg / 5 ml, granules for oral susp., 150 ml, bot. [fr:100 mg / 5 ml, granules susp. orale, 150 ml, fl.]", Unit.ML),
            new Format("DORALINE6T", "600 mg, tab. [fr:600 mg, comp.]", Unit.TABLET),
            new Format("DORALINE6TB1", "600 mg, breakable tab., blister [fr:600 mg, comp. sécable, blister]", Unit.TABLET)
        ),
        new Drug("DORALOPE", "LOPERAMIDE hydrochloride [fr:LOPERAMIDE chlorhydrate]").withFormats(
            new Format("DORALOPE2C", "2 mg, caps. [fr:2 mg, gél.]", Unit.CAPSULE),
            new Format("DORALOPE2T", "2 mg, tab. [fr:2 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALORA", "LORATADINE [fr:LORATADINE]").withFormats(
            new Format("DORALORA1S", "5 mg / 5 ml, oral sol., 100 ml, bot. [fr:5 mg / 5 ml, sol. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORALORA1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORALOSA", "LOSARTAN potassium [fr:LOSARTAN potassium]").withFormats(
            new Format("DORALOSA5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET),
            new Format("DORALOSA5TB", "50 mg, breakable tab. [fr:50 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORALPVR", "LPV / r [fr:LPV / r]").withFormats(
            new Format("DORALPVR1G", "40 mg / 10 mg, granules, sachet [fr:40 mg / 10 mg, granules, sachet]", Unit.MG),
            new Format("DORALPVR1P", "40 mg / 10 mg, pellets-in-a-capsule [fr:40 mg / 10 mg, granules dans gélule]", Unit.MG),
            new Format("DORALPVR2S", "/ 400/100 mg / 5 ml, oral sol., 60 ml, bot. [fr:/ 400/100 mg / 5 ml, sol. orale, 60 ml, fl.]", Unit.ML),
            new Format("DORALPVR3S", "/ 400/100 mg / 5 ml, oral sol., 160 ml, bot. [fr:/ 400/100 mg / 5 ml, sol. orale, 160 ml, fl.]", Unit.ML),
            new Format("DORALPVR4T", "100 mg / 25 mg, tab. [fr:100 mg / 25 mg, comp.]", Unit.TABLET),
            new Format("DORALPVR5T", "200 mg / 50 mg, tab. [fr:200 mg / 50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMAGN", "MAGNESIUM oxide [fr:Oxyde de MAGNESIUM]").withFormats(
            new Format("DORAMAGN1TE", "270 mg, eq. to 150 mg Mg, efferv. tab. [fr:270 mg, éq. 150 mg Mg, comp. efferv.]", Unit.TABLET),
            new Format("DORAMAGN3T", "eq. to 300 mg Mg, tab. [fr:éq. à 300 mg Mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMAGP", "MAGNESIUM lactate / PYRIDOXINE HCl [fr:MAGNESIUM lactate / PYRIDOXINE HCl]").withFormats(
            new Format("DORAMAGP55T", "eq. 48 mg Mg / 5 mg, tab. [fr:éq. 48 mg Mg / 5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMEBE", "MEBENDAZOLE [fr:MEBENDAZOLE]").withFormats(
            new Format("DORAMEBE1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMEDR", "MEDROXYPROGESTERONE acetate [fr:MEDROXYPROGESTERONE acétate]").withFormats(
            new Format("DORAMEDR1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMEFL", "MEFLOQUINE hydrochloride [fr:MEFLOQUINE chlorhydrate]").withFormats(
            new Format("DORAMEFL2T", "eq. 250 mg base, tab. [fr:éq. 250 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMETF", "METFORMIN hydrochloride [fr:METFORMINE chlorhydrate]").withFormats(
            new Format("DORAMETF1T", "1000 mg, tab. [fr:1000 mg, comp.]", Unit.TABLET),
            new Format("DORAMETF5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET),
            new Format("DORAMETF8T", "850 mg, tab. [fr:850 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMETN", "METRONIDAZOLE [fr:METRONIDAZOLE]").withFormats(
            new Format("DORAMETN2S", "benzoate, eq. 200 mg / 5 ml base, oral susp., 100 ml [fr:benzoate, éq. 200 mg / 5 ml base, susp. orale, 100 ml]", Unit.ML),
            new Format("DORAMETN2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORAMETN5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMETO", "METOCLOPRAMIDE hydrochloride anhydrous [fr:METOCLOPRAMIDE chlorhydrate anhydre]").withFormats(
            new Format("DORAMETO1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMETY", "METHYLDOPA [fr:METHYLDOPA]").withFormats(
            new Format("DORAMETY2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMICO", "MICONAZOLE [fr:MICONAZOLE]").withFormats(
            new Format("DORAMICO2J1", "nitrate, 2%, oral gel, 15 g, tube [fr:nitrate, 2%, gel oral, 15 g, tube]", Unit.ML),
            new Format("DORAMICO2J4", "2%, oral gel, 40 g, tube [fr:2%, gel oral, 40 g, tube]", Unit.ML),
            new Format("DORAMICO2J8", "2%, oral gel, 80 g, tube [fr:2%, gel oral, 80 g, tube]", Unit.ML)
        ),
        new Drug("DORAMIFP", "MIFEPRISTONE [fr:MIFEPRISTONE]").withFormats(
            new Format("DORAMIFP2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMILT", "MILTEFOSINE [fr:MILTEFOSINE]").withFormats(
            new Format("DORAMILT1C", "10 mg, caps. [fr:10 mg, gél.]", Unit.CAPSULE),
            new Format("DORAMILT5C", "50 mg, caps. [fr:50 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAMISP", "MISOPROSTOL [fr:MISOPROSTOL]").withFormats(
            new Format("DORAMISP25T", "25 µg, tab. [fr:25 µg, comp.]", Unit.TABLET),
            new Format("DORAMISP2T", "200 µg, tab. [fr:200 µg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMMNS", "MULTIPLE MICRONUTRIENTS SUPPLEMENTS [fr:MICRONUTRIMENTS MULTIPLES, SUPPLEMENTS]").withFormats(
            new Format("DORAMMNS1T", "tab. [fr:S ^, comp.]", Unit.TABLET)
        ),
        new Drug("DORAMONT", "MONTELUKAST [fr:MONTELUKAST]").withFormats(
            new Format("DORAMONT5TC", "5 mg, chewing tab. [fr:5 mg, tab. à macher]", Unit.TABLET)
        ),
        new Drug("DORAMORP", "MORPHINE sulfate [fr:MORPHINE sulfate]").withFormats(
            new Format("DORAMORP1CS", "10 mg, prolonged-release caps. [fr:10 mg, gél. libération prolongée]", Unit.CAPSULE),
            new Format("DORAMORP1S", "10 mg / 5 ml, oral sol., 100 ml, bot. [fr:10 mg / 5 ml, sol. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORAMORP1T", "10 mg, immediate release breakable tab. [fr:10 mg, comp. sécable libération immédiate]", Unit.TABLET),
            new Format("DORAMORP1TS", "10 mg, prolonged-release tab. [fr:10 mg, comp. libération prolongée]", Unit.TABLET),
            new Format("DORAMORP3CS", "30 mg, prolonged-release caps. [fr:30 mg, gél. libération prolongée]", Unit.CAPSULE),
            new Format("DORAMORP3TS", "30 mg, prolonged-release, tab. [fr:30 mg, comp. libération prolongée]", Unit.TABLET)
        ),
        new Drug("DORAMOXI", "MOXIFLOXACIN hydrochloride [fr:MOXIFLOXACINE chlorhydrate]").withFormats(
            new Format("DORAMOXI1TD", "eq. 100 mg base, disp. tab. [fr:éq. 100 mg base, comp. disp.]", Unit.TABLET),
            new Format("DORAMOXI4T1", "eq 400 mg base, tab. blister [fr:éq 400 mg base, comp. blister]", Unit.TABLET)
        ),
        new Drug("DORAMULT", "MULTIVITAMINS [fr:MULTIVITAMINES]").withFormats(
            new Format("DORAMULT1T", "tab. [fr:comp.]", Unit.TABLET)
        ),
        new Drug("DORANEVI", "NEVIRAPINE (NVP) [fr:NEVIRAPINE (NVP)]").withFormats(
            new Format("DORANEVI1S1", "50 mg / 5 ml, oral susp., 100 ml, bot. [fr:50 mg / 5 ml, susp. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORANEVI1S2", "50 mg / 5 ml, oral susp., 240 ml, bot. [fr:50 mg / 5 ml, susp. orale, 240 ml, fl.]", Unit.ML),
            new Format("DORANEVI2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORANEVI5TD", "50 mg, disp. tab. [fr:50 mg, comp. disp.]", Unit.TABLET)
        ),
        new Drug("DORANICA", "NICARDIPINE hydrochloride [fr:NICARDIPINE chlorhydrate]").withFormats(
            new Format("DORANICA2TB", "20 mg, breakable tab. [fr:20 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORANICO", "NICOTINAMIDE (vitamin PP) [fr:NICOTINAMIDE (vitamine PP)]").withFormats(
            new Format("DORANICO1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORANIFE", "NIFEDIPINE [fr:NIFEDIPINE]").withFormats(
            new Format("DORANIFE1C", "10 mg, immediate release soft caps. [fr:10 mg, caps. molle lib. immédiate]", Unit.CAPSULE),
            new Format("DORANIFE1TI", "10 mg, immediate release tab. [fr:10 mg, comp. lib. immédiate]", Unit.TABLET)
        ),
        new Drug("DORANIFU", "NIFURTIMOX [fr:NIFURTIMOX]").withFormats(
            new Format("DORANIFU1T", "120 mg, tab. [fr:120 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORANYST", "NYSTATIN [fr:NYSTATINE]").withFormats(
            new Format("DORANYST1S", "100.000 IU / ml, oral susp. [fr:100.000 UI / ml, susp. orale]", Unit.MG)
        ),
        new Drug("DORAOLAN", "OLANZAPINE [fr:OLANZAPINE]").withFormats(
            new Format("DORAOLAN2T", "2.5 mg, tab. [fr:2.5 mg, comp.]", Unit.TABLET),
            new Format("DORAOLAN5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAOMEP", "OMEPRAZOLE [fr:OMEPRAZOLE]").withFormats(
            new Format("DORAOMEP1TDG", "10 mg, disp. gastro-resistant tab. [fr:10 mg, comp. disp. gastro-résistant]", Unit.TABLET),
            new Format("DORAOMEP2CG", "20 mg, gastro-resistant caps. [fr:20 mg, gél. gastrorésistante]", Unit.CAPSULE)
        ),
        new Drug("DORAONDA", "ONDANSETRON [fr:ONDANSETRON]").withFormats(
            new Format("DORAONDA1S", "HCl, eq. 4 mg / 5 ml base, oral sol., 50 ml, bot. [fr:HCl, éq. 4 mg / 5 ml base, sol. orale, 50 ml, fl.]", Unit.ML),
            new Format("DORAONDA4T", "hydrochloride, eq. 4 mg base, tab. [fr:chlorhydrate, éq. 4 mg base, comp.]", Unit.TABLET),
            new Format("DORAONDA8T", "hydrochloride, eq. 8 mg base, tab. [fr:chlorhydrate, éq. 8 mg base, comp]", Unit.TABLET)
        ),
        new Drug("DORAORMA", "RESOMAL [fr:RESOMAL]").withFormats(
            new Format("DORAORMA2S8", "rehydration acute complic. malnut., sach. 84 g / 2 l [fr:réhydratation malnut. aiguë compliq, sach. 84 g / 2 l]", Unit.MG)
        ),
        new Drug("DORAORSA", "ORAL REHYDRATION SALTS (ORS) low osmol. [fr:SELS REHYDRATATION ORALE (SRO) basse osmol.]").withFormats(
            new Format("DORAORSA2S", "sachet 20.5 g / 1 l [fr:sachet 20.5 g / 1 l]", Unit.ML)
        ),
        new Drug("DORAOSEL", "OSELTAMIVIR phosphate [fr:OSELTAMIVIR phosphate]").withFormats(
            new Format("DORAOSEL7C", "eq. 75 mg base, caps. [fr:éq. 75 mg base, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORAPARA", "PARACETAMOL (acetaminophen) [fr:PARACETAMOL (acétaminophène)]").withFormats(
            new Format("DORAPARA1S2", "120 mg / 5 ml, oral susp., 100 ml bot. [fr:120 mg / 5 ml, susp. orale, 100 ml fl.]", Unit.ML),
            new Format("DORAPARA1T", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET),
            new Format("DORAPARA5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPARX", "PAROXETINE [fr:PAROXETINE]").withFormats(
            new Format("DORAPARX2TB", "20 mg, breakable tab. [fr:20 mg, comp. sécable]", Unit.TABLET)
        ),
        new Drug("DORAPASA", "PARA-AMINOSALICYLIC acid (PAS) [fr:Acide PARA-AMINOSALICYLIQUE (PAS), gran. lib.]").withFormats(
            new Format("DORAPASA4S", "delayed rel. gran, 4 g, sach. [fr:. ret., 4 g, sach.]", Unit.MG),
            new Format("DORAPASA4S2", "del. rel. gran, 4 g, sach. (25°C) [fr:. ret, 4 g, sach(25°C)]", Unit.MG)
        ),
        new Drug("DORAPASS", "PARA-AMINOSALICYLATE sodium [fr:PARA-AMINOSALICYLATE sodique]").withFormats(
            new Format("DORAPASS1", "del. rel. gran 60% w / w, 100 g jar [fr:gran. lib. prol 60% w / w, 100 g pot]", Unit.ML),
            new Format("DORAPASS5S", "5.52 g, powder oral sol., sach. [fr:5.52 g, poudre sol. orale, sach]", Unit.MG),
            new Format("DORAPASS9S", "del. rel. gran 60% w / w, 9.2 g sach. [fr:gran. lib. prol 60% w / w, 9.2 g sach]", Unit.ML)
        ),
        new Drug("DORAPEGL", "POLYETHYLENE GLYCOL [fr:POLYETHYLENE GLYCOL]").withFormats(
            new Format("DORAPEGL1P", "powder, sachet [fr:poudre, sachet]", Unit.MG)
        ),
        new Drug("DORAPENV", "PHENOXYMETHYLPENICILLIN [fr:PHENOXYMETHYLPENICILLINE]").withFormats(
            new Format("DORAPENV1S1", "125 mg / 5 ml, powd. oral sol, 100 ml, bot [fr:125 mg / 5 ml, poudre sol. orale, 100 ml, fl]", Unit.ML),
            new Format("DORAPENV2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPHEN", "PHENOBARBITAL [fr:PHENOBARBITAL]").withFormats(
            new Format("DORAPHEN1T", "15 mg, tab. [fr:15 mg, comp.]", Unit.TABLET),
            new Format("DORAPHEN3T", "30 mg, tab. [fr:30 mg, comp.]", Unit.TABLET),
            new Format("DORAPHEN5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET),
            new Format("DORAPHEN6T", "60 mg, tab. [fr:60 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPHEY", "PHENYTOIN [fr:PHENYTOINE]").withFormats(
            new Format("DORAPHEY1S", "30 mg / 5 ml, oral susp., 500 ml, bot. [fr:30 mg / 5 ml, susp. orale, 500 ml, fl.]", Unit.ML),
            new Format("DORAPHEY1T", "sodium, 100 mg, tab. [fr:sodique, 100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPHLO", "PHLOROGLUCINOL [fr:PHLOROGLUCINOL]").withFormats(
            new Format("DORAPHLO8TOD", "80 mg, orodisp. tab. [fr:80 mg, comp. orodisp.]", Unit.TABLET)
        ),
        new Drug("DORAPHYT", "PHYTOMENADIONE (vitamin K1) [fr:PHYTOMENADIONE (vitamine K1)]").withFormats(
            new Format("DORAPHYT1A1", "10 mg / ml, 1 ml, amp. [fr:10 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DORAPOTC", "POTASSIUM chloride [fr:POTASSIUM chlorure]").withFormats(
            new Format("DORAPOTC6TP", "600 mg (8mEq), prolonged-release tab. [fr:600 mg (8mEq), comp. libération prolongée]", Unit.TABLET),
            new Format("DORAPOTC7S", "7.5% w / v, 1mmol K / ml, oral sol., 500 ml, bot [fr:7.5% p / v, 1mmol K / ml, sol. orale, 500 ml, fl]", Unit.ML)
        ),
        new Drug("DORAPRAZ", "PRAZIQUANTEL [fr:PRAZIQUANTEL]").withFormats(
            new Format("DORAPRAZ6TB", "600 mg, break. tab. [fr:600 mg, comp. séc.]", Unit.TABLET)
        ),
        new Drug("DORAPRED", "PREDNISOLONE [fr:PREDNISOLONE]").withFormats(
            new Format("DORAPRED2TOD", "20 mg, orodisp. tablet [fr:20 mg, comp. orodisp.]", Unit.MG),
            new Format("DORAPRED5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPRIM", "PRIMAQUINE diphosphate [fr:PRIMAQUINE diphosphate]").withFormats(
            new Format("DORAPRIM1T", "eq. 15 mg base, tab. [fr:éq. 15 mg base, comp.]", Unit.TABLET),
            new Format("DORAPRIM7T", "eq. 7.5 mg base, tab. [fr:éq. 7.5 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPRIS", "PRISTINAMYCIN [fr:PRISTINAMYCINE]").withFormats(
            new Format("DORAPRIS5TB", "500 mg, break. tab. [fr:500 mg, comp. séc.]", Unit.TABLET)
        ),
        new Drug("DORAPROM", "PROMETHAZINE [fr:PROMETHAZINE]").withFormats(
            new Format("DORAPROM2T", "hydrochloride, eq. 25 mg base, tab. [fr:chlorhydrate, éq. 25 mg base, comp.]", Unit.TABLET),
            new Format("DORAPROM5S", "5 mg / 5 ml, syrup, 150 ml, bot. [fr:5 mg / 5 ml, sirop, 150 ml, fl.]", Unit.ML),
            new Format("DORAPROM5S1", "5 mg / 5 ml, oral solution, 100 ml, bot [fr:5 mg / 5 ml, sol. orale, 100 ml, fl.]", Unit.ML)
        ),
        new Drug("DORAPRON", "PROTHIONAMIDE [fr:PROTHIONAMIDE]").withFormats(
            new Format("DORAPRON2T1", "250 mg, tab., blister [fr:250 mg, comp., blister]", Unit.TABLET)
        ),
        new Drug("DORAPYRA", "PYRANTEL [fr:PYRANTEL]").withFormats(
            new Format("DORAPYRA1S", "250 mg / 5 ml, oral susp., 15 ml, bot. [fr:250 mg / 5 ml, susp. orale, 15 ml, fl.]", Unit.ML)
        ),
        new Drug("DORAPYRI", "PYRIDOXINE hydrochloride (vitamin B6) [fr:PYRIDOXINE chlorhydrate (vitamine B6)]").withFormats(
            new Format("DORAPYRI1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET),
            new Format("DORAPYRI5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPYRM", "PYRIMETHAMINE [fr:PYRIMETHAMINE]").withFormats(
            new Format("DORAPYRM2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAPYRZ", "PYRAZINAMIDE (Z) [fr:PYRAZINAMIDE (Z)]").withFormats(
            new Format("DORAPYRZ1T1", "150 mg, disp. tab., blister [fr:150 mg, comp. disp., blister]", Unit.TABLET),
            new Format("DORAPYRZ1T3", "150 mg, disp. tab., bulk [fr:150 mg, comp. disp., vrac]", Unit.TABLET),
            new Format("DORAPYRZ4T1", "400 mg, tab., blister [fr:400 mg, comp., blister]", Unit.TABLET),
            new Format("DORAPYRZ4T3", "400 mg, tab., bulk [fr:400 mg, comp., vrac]", Unit.TABLET)
        ),
        new Drug("DORAQUIN", "QUININE sulfate [fr:QUININE sulfate]").withFormats(
            new Format("DORAQUIN3T", "300 mg, tab. [fr:300 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORARALT", "RALTEGRAVIR potassium (RAL) [fr:RALTEGRAVIR potassique (RAL)]").withFormats(
            new Format("DORARALT1TC", "eq. 100 mg base, chew. tab. [fr:éq. 100 mg base, comp. à macher]", Unit.TABLET),
            new Format("DORARALT2TC", "eq. 25 mg base, chew. tab. [fr:éq. 25 mg base, comp. à mâcher]", Unit.TABLET),
            new Format("DORARALT4T", "eq. 400 mg base, tab. [fr:éq. 400 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORARAMI", "RAMIPRIL [fr:RAMIPRIL]").withFormats(
            new Format("DORARAMI1T", "10 mg, tab. [fr:10 mg, comp.]", Unit.TABLET),
            new Format("DORARAMI2T", "2.5 mg, tab. [fr:2.5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORARANI", "RANITIDINE [fr:RANITIDINE]").withFormats(
            new Format("DORARANI1T", "150 mg, tab. [fr:150 mg, comp.]", Unit.TABLET),
            new Format("DORARANI1TE", "150 mg, effervescent tab. [fr:150 mg, comp. effervescent]", Unit.TABLET)
        ),
        new Drug("DORARETI", "RETINOL (vitamin A) stabil. [fr:RETINOL (vitamine A) stabilisé]").withFormats(
            new Format("DORARETI2C", "200.000 IU, soft gelat. caps. [fr:200.000 UI, caps. molle]", Unit.CAPSULE)
        ),
        new Drug("DORARIBA", "RIBAVIRIN [fr:RIBAVIRINE]").withFormats(
            new Format("DORARIBA1S", "200 mg / 5 ml, oral sol., 100 ml, bot. [fr:200 mg / 5 ml, sol. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORARIBA2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE),
            new Format("DORARIBA2T", "200 mg, tab. [fr:200 mg, comp.]", Unit.TABLET),
            new Format("DORARIBA4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORARIFA", "RIFAMPICIN (R) [fr:RIFAMPICINE (R)]").withFormats(
            new Format("DORARIFA1C1", "150 mg, caps. blister [fr:150 mg, gél. blister]", Unit.CAPSULE),
            new Format("DORARIFA1C3", "150 mg, caps. bulk [fr:150 mg, gél. vrac]", Unit.CAPSULE),
            new Format("DORARIFA1T1", "150 mg, tab., blister [fr:150 mg, comp., blister]", Unit.TABLET),
            new Format("DORARIFA1T3", "150 mg, tab., bulk [fr:150 mg, comp., vrac]", Unit.TABLET),
            new Format("DORARIFA3C1", "300 mg, caps. blister [fr:300 mg, gél. blister]", Unit.CAPSULE),
            new Format("DORARIFA3C3", "300 mg, caps. bulk [fr:300 mg, gél. vrac]", Unit.CAPSULE)
        ),
        new Drug("DORARIFB", "RIFABUTIN [fr:RIFABUTINE]").withFormats(
            new Format("DORARIFB1C", "150 mg, caps. [fr:150 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORARIFP", "RIFAPENTINE [fr:RIFAPENTINE]").withFormats(
            new Format("DORARIFP1T1", "150 mg, tab., blister [fr:150 mg, comp., blister]", Unit.TABLET)
        ),
        new Drug("DORARISP", "RISPERIDONE [fr:RISPERIDONE]").withFormats(
            new Format("DORARISP1T", "1 mg, tab. [fr:1 mg, comp.]", Unit.TABLET),
            new Format("DORARISP2T", "2 mg, tab. [fr:2 mg, comp.]", Unit.TABLET),
            new Format("DORARISP4T", "4 mg, tab. [fr:4 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORARITO", "RITONAVIR (r) [fr:RITONAVIR (r)]").withFormats(
            new Format("DORARITO1T2", "100 mg, tab. [fr:100 mg, comp.]", Unit.TABLET),
            new Format("DORARITO2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET),
            new Format("DORARITO8S", "400 mg / 5 ml, oral sol., 90 ml, bot. [fr:400 mg / 5 ml, sol. orale, 90 ml, fl.]", Unit.ML)
        ),
        new Drug("DORASALB", "SALBUTAMOL [fr:SALBUTAMOL]").withFormats(
            new Format("DORASALB1N", "solution for nebulizer, 2 mg / ml, 2.5 ml monodose [fr:solution pour nébuliseur, 2 mg / ml, 2.5 ml unidose]", Unit.ML),
            new Format("DORASALB2SF", "sulfate, eq. 0.1 mg base / puff, 200 puffs, aerosol [fr:sulfate, éq. 0.1 mg base / bouffée, 200 bouff. aérosol]", Unit.PUFF)
        ),
        new Drug("DORASALM", "SALMETEROL [fr:SALMETEROL]").withFormats(
            new Format("DORASALM2SF", "25 µg / puff, 120 puffs, aerosol [fr:25 µg / bouffée, 120 bouffées, aerosol]", Unit.PUFF)
        ),
        new Drug("DORASERT", "SERTRALINE hydrochloride [fr:SERTRALINE chlorhydrate]").withFormats(
            new Format("DORASERT1T", "eq. 100 mg base, tab. [fr:éq. 100 mg base, comp.]", Unit.TABLET),
            new Format("DORASERT5T", "eq. 50 mg base, tab. [fr:éq. 50 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORASODC", "SODIUM chloride [fr:SODIUM chlorure]").withFormats(
            new Format("DORASODC6V", "6%, for nebulizer, 4 ml, vial [fr:6%, pour nébulisation, 4 ml, fl.]", Unit.ML)
        ),
        new Drug("DORASOFO", "SOFOSBUVIR [fr:SOFOSBUVIR]").withFormats(
            new Format("DORASOFO4T", "400 mg, tab. [fr:400 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORASOVE", "SOF / VEL (SOFOSBUVIR / VELPATASVIR) [fr:SOF / VEL (SOFOSBUVIR / VELPATASVIR)]").withFormats(
            new Format("DORASOVE41T", "() 400 mg / () 100 mg, tab. [fr:() 400 mg / () 100 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORASOVV", "SOF / VEL / VOX (SOFOSBUVIR / VELPATASVIR / VOXILAPREVIR) [fr:SOF / VEL / VOX (SOFOSBUVIR / VELPATASVIR / VOXILAPREVIR)]").withFormats(
            new Format("DORASOVV411T", "400 mg / 100 mg / 100 mg, tab. [fr:400 mg / 100 mg / 100 mg, comp]", Unit.TABLET)
        ),
        new Drug("DORASPAQ", "SP + AQ (SULFADOXINE / PYRIMETHAMINE + AMODIAQUINE) [fr:SP + AQ (SULFADOXINE / PYRIMETHAMINE + AMODIAQUINE)]").withFormats(
            new Format("DORASPAQ1TD2", "1 x 250/12.5 mg+ 3xeq. 75-76.5 mg base, cobl. disp. tab, 4.5-8kg [fr:1 x 250/12.5 mg+ 3 x eq. 75-76.5 mg base, cobl. comp. disp., 4.5-8kg]", Unit.TABLET),
            new Format("DORASPAQ2TD2", "1 x 500/25 mg + 3 x eq. 150-153 mg base, cobl. disp. tab, 9-17kg [fr:1 x 500/25 mg+ 3 x éq. 150-153 mg base, cobl. comp. disp, 9-17kg]", Unit.TABLET)
        ),
        new Drug("DORASPIR", "SPIRONOLACTONE [fr:SPIRONOLACTONE]").withFormats(
            new Format("DORASPIR2T", "25 mg, tab. [fr:25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORASUCC", "SUCCIMER [fr:SUCCIMER]").withFormats(
            new Format("DORASUCC2C", "200 mg, caps. [fr:200 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORASUDI", "SULFADIAZINE [fr:SULFADIAZINE]").withFormats(
            new Format("DORASUDI5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORASULP", "SULFADOXINE / PYRIMETHAMINE [fr:SULFADOXINE / PYRIMETHAMINE]").withFormats(
            new Format("DORASULP5T", "500 mg / 25 mg, tab. [fr:500 mg / 25 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATEEF", "TDF / FTC / EFV [fr:TDF / FTC / EFV]").withFormats(
            new Format("DORATEEF1T", "300 mg / 200 mg / 600 mg, tab. [fr:300 mg / 200 mg / 600 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATEEM", "TDF / FTC [fr:TDF / FTC]").withFormats(
            new Format("DORATEEM1T", "300 mg / 200 mg, tab. [fr:300 mg / 200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATELA", "TDF / 3TC [fr:TDF / 3TC]").withFormats(
            new Format("DORATELA1T", "300 mg / 300 mg, tab. [fr:300 mg / 300 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATELD", "TDF / 3TC / DTG [fr:TDF / 3TC / DTG]").withFormats(
            new Format("DORATELD1T", "300 mg / 300 mg / 50 mg, tab. [fr:300 mg / 300 mg / 50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATELE", "TDF / 3TC / EFV [fr:TDF / 3TC / EFV]").withFormats(
            new Format("DORATELE1T", "300 mg / 300 mg / 600 mg, tab. [fr:300 mg / 300 mg / 600 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATENO", "TENOFOVIR DISOPROXIL [fr:TENOFOVIR DISOPROXIL]").withFormats(
            new Format("DORATENO2T", "FUMARATE, eq. 163 mg base, tab. [fr:FUMARATE, éq. 163 mg base, comp.]", Unit.TABLET),
            new Format("DORATENO3T", "fumarate 300 mg, eq. 245 mg base, tab. [fr:fumarate 300 mg, éq. 245 mg base, comp]", Unit.TABLET)
        ),
        new Drug("DORATHIA", "THIAMINE hydrochloride (vitamin B1) [fr:THIAMINE chlorhydrate (vitamine B1)]").withFormats(
            new Format("DORATHIA2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET),
            new Format("DORATHIA5T", "50 mg, tab. [fr:50 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATINI", "TINIDAZOLE [fr:TINIDAZOLE]").withFormats(
            new Format("DORATINI5T", "500 mg, tab. [fr:500 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATRAM", "TRAMADOL hydrochloride [fr:TRAMADOL chlorhydrate]").withFormats(
            new Format("DORATRAM1S", "100 mg / ml / 40 drops, 10 ml, bot. [fr:100 mg / ml / 40 gouttes, 10 ml, fl.]", Unit.ML),
            new Format("DORATRAM5C", "50 mg, caps. [fr:50 mg, gél.]", Unit.CAPSULE)
        ),
        new Drug("DORATRAN", "TRANEXAMIC ACID [fr:ACIDE TRANEXAMIQUE]").withFormats(
            new Format("DORATRAN5T", "500 mg tab [fr:500 mg comp]", Unit.TABLET)
        ),
        new Drug("DORATRIB", "TRICLABENDAZOLE [fr:TRICLABENDAZOLE]").withFormats(
            new Format("DORATRIB2T", "250 mg, tab. [fr:250 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORATRIH", "TRIHEXYPHENIDYL hydrochloride [fr:TRIHEXYPHENIDYLE chlorhydrate]").withFormats(
            new Format("DORATRIH2T", "2 mg, tab. [fr:2 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAULIP", "ULIPRISTAL acetate [fr:ULIPRISTAL acétate]").withFormats(
            new Format("DORAULIP3T", "30 mg, tab. [fr:30 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAVALG", "VALGANCICLOVIR hydrochloride [fr:VALGANCICLOVIR chlorhydrate]").withFormats(
            new Format("DORAVALG4T", "eq. 450 mg base, tab. [fr:éq. 450 mg base, comp.]", Unit.TABLET)
        ),
        new Drug("DORAVALP", "VALPROATE SODIUM [fr:VALPROATE de SODIUM]").withFormats(
            new Format("DORAVALP1S", "200 mg / ml, 40 ml, bot. + syringe [fr:200 mg / ml, 40 ml, fl. + seringue]", Unit.ML),
            new Format("DORAVALP2S", "200 mg / 5 ml, 300 ml, bot. [fr:200 mg / 5 ml, 300 ml, fl.]", Unit.ML),
            new Format("DORAVALP2TG", "200 mg, gastro-resistant tab. [fr:200 mg, comp. gastro-résistant]", Unit.TABLET),
            new Format("DORAVALP5TG", "500 mg, gastro-resistant tab. [fr:500 mg, comp. gastro-résistant]", Unit.TABLET)
        ),
        new Drug("DORAVERA", "VERAPAMIL hydrochloride [fr:VERAPAMIL chlorhydrate]").withFormats(
            new Format("DORAVERA4T", "40 mg, tab. [fr:40 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAVITB", "VITAMINE B COMPLEX [fr:VITAMINE B COMPLEX]").withFormats(
            new Format("DORAVITB1T", "tab. [fr:comp.]", Unit.TABLET)
        ),
        new Drug("DORAWARF", "WARFARIN [fr:WARFARINE]").withFormats(
            new Format("DORAWARF1T", "1 mg, tab. [fr:1 mg, comp.]", Unit.TABLET),
            new Format("DORAWARF5T", "5 mg, tab. [fr:5 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAYIDO", "ZIDOVUDINE (AZT) [fr:ZIDOVUDINE (AZT)]").withFormats(
            new Format("DORAYIDO1S", "50 mg / 5 ml, oral sol., 100 ml bot. [fr:50 mg / 5 ml, sol. orale, 100 ml, fl.]", Unit.ML),
            new Format("DORAYIDO2S", "50 mg / 5 ml, oral sol., 200 ml, bot. [fr:50 mg / 5 ml, sol. orale, 200 ml, fl.]", Unit.ML),
            new Format("DORAYIDO3S", "50 mg / 5 ml, oral sol., 240 ml, bot. [fr:50 mg / 5 ml, sol. orale, 240 ml, fl.]", Unit.ML),
            new Format("DORAYIDO3T", "300 mg, tab. [fr:300 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAYILA", "AZT / 3TC [fr:AZT / 3TC]").withFormats(
            new Format("DORAYILA1T", "60 mg / 30 mg, tab. [fr:60 mg / 30 mg, comp.]", Unit.TABLET),
            new Format("DORAYILA1TD", "60 mg / 30 mg, disp. tab. [fr:60 mg / 30 mg, comp. disp.]", Unit.TABLET),
            new Format("DORAYILA2T", "300 mg / 150 mg, tab. [fr:300 mg / 150 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAYILE", "AZT / 3TC + EFV [fr:AZT / 3TC + EFV]").withFormats(
            new Format("DORAYILE1T2", "300 mg / 150 mg x 2 + 600 mg x 1, coblister of tab [fr:300 mg / 150 mg x 2 + 600 mg x 1, coblister comp]", Unit.TABLET),
            new Format("DORAYILE1T4", "300 mg / 150 mg x 60 + 600 mg x 30, co-pack of tab. [fr:300 mg / 150 mg x 60 + 600 mg x 30, co-pack comp.]", Unit.TABLET)
        ),
        new Drug("DORAYILN", "AZT / 3TC / NVP [fr:AZT / 3TC / NVP]").withFormats(
            new Format("DORAYILN1TD", "60 mg / 30 mg / 50 mg, dispersible tab. [fr:60 mg / 30 mg / 50 mg, comp. dispersible]", Unit.TABLET),
            new Format("DORAYILN2T", "300 mg / 150 mg / 200 mg, tab. [fr:300 mg / 150 mg / 200 mg, comp.]", Unit.TABLET)
        ),
        new Drug("DORAYINS", "ZINC sulfate [fr:ZINC sulfate]").withFormats(
            new Format("DORAYINS2T", "eq. to 20 mg zinc mineral, dispersible tab. [fr:éq. à 20 mg de zinc minéral, comp. dispers.]", Unit.TABLET)
        ),
        new Drug("DORADEXT", "DEXTROSE (GLUCOSE) [fr:GLUCOSE]").withFormats(
            new Format("DORADEXTXX5", "5%, oral (nonstandard) [fr:5%, orale (non standard)]", Unit.ML),
            new Format("DORADEXTXX10", "10%, oral (nonstandard) [fr:10%, orale (non standard)]", Unit.ML)
        )
    );

    Category INJECTABLE = new Category("DINJ", "injectable", false, IV, SC, IM, IO).withDrugs(
        new Drug("DINJACCY", "ACETYLCYSTEINE [fr:ACETYLCYSTEINE]").withFormats(
            new Format("DINJACCY2A", "200 mg / ml, 10 ml, amp. [fr:200 mg / ml, 10 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJACIV", "ACICLOVIR sodium [fr:ACICLOVIR sodique]").withFormats(
            new Format("DINJACIV2V", "eq. 250 mg base, powder, vial [fr:éq. 250 mg base, poudre, fl]", Unit.MG),
            new Format("DINJACIV2V1", "eq. 25 mg / ml base, 10 ml, vial [fr:éq. 25 mg / ml base, 10 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJADEN", "ADENOSINE [fr:ADENOSINE]").withFormats(
            new Format("DINJADEN6V", "3 mg / ml, 2 ml, vial [fr:3 mg / ml, 2 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJAMBC", "AMPHOTERICIN B conventional [fr:AMPHOTERICINE B conventionnelle]").withFormats(
            new Format("DINJAMBC5V", "50 mg, powder, vial [fr:50 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJAMBL", "AMPHOTERICIN B liposomal complex [fr:AMPHOTERICINE B complexe liposomal]").withFormats(
            new Format("DINJAMBL5V", "50 mg, powder, vial [fr:50 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJAMIK", "AMIKACIN sulfate [fr:AMIKACINE sulfate]").withFormats(
            new Format("DINJAMIK5A", "eq. 250 mg / ml base, 2 ml, amp. [fr:éq. 250 mg / ml base, 2 ml, amp.]", Unit.ML),
            new Format("DINJAMIK5V1", "eq. 250 mg / ml base, 2 ml, vial [fr:éq. 250 mg / ml base, 2 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJAMIO", "AMIODARONE hydrochloride [fr:AMIODARONE chlorhydrate]").withFormats(
            new Format("DINJAMIO1A", "50 mg / ml, 3 ml, amp. [fr:50 mg / ml, 3 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJAMOC", "AMOXICILLIN / CLAVULANIC acid [fr:AMOXICILLINE / acide CLAVULANIQUE]").withFormats(
            new Format("DINJAMOC1V2", "1 g / 200 mg, powder [fr:1 g / 200 mg, poudre]", Unit.MG)
        ),
        new Drug("DINJAMPI", "AMPICILLIN [fr:AMPICILLINE]").withFormats(
            new Format("DINJAMPI1V", "1 g, powder, vial [fr:1 g, poudre, fl.]", Unit.MG),
            new Format("DINJAMPI5V", "500 mg, powder, vial [fr:500 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJAREP", "ARTICAINE / EPINEPHRINE [fr:ARTICAINE / EPINEPHRINE]").withFormats(
            new Format("DINJAREP4C1", "4% / 1/100000, 1.7 ml, dent. cartr. [fr:4% / 1/100000, 1.7 ml carp. dent.]", Unit.ML)
        ),
        new Drug("DINJARTS", "ARTESUNATE [fr:ARTESUNATE]").withFormats(
            new Format("DINJARTS6V", "60 mg, powder, vial +NaHCO3 5% 1 ml +NaCl 0.9% 5 ml [fr:60 mg, poudre, fl +NaHCO3 5% 1 ml +NaCl 0.9% 5 ml]", Unit.MG)
        ),
        new Drug("DINJATRB", "ATRACURIUM besilate [fr:besilate d'ATRACURIUM]").withFormats(
            new Format("DINJATRB2A", "10 mg / ml, 2.5 ml, amp. [fr:10 mg / ml, 2.5 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJATRO", "ATROPINE sulfate [fr:ATROPINE sulfate]").withFormats(
            new Format("DINJATRO1A", "1 mg / ml, 1 ml, amp. [fr:1 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJBLEO", "BLEOMYCIN sulfate [fr:BLEOMYCINE sulfate]").withFormats(
            new Format("DINJBLEO1V", "eq 15.000 IU base, powder, vial [fr:éq. 15.000 UI base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJBUPI", "BUPIVACAINE HCl, hyperbaric / spinal [fr:BUPIVACAINE HCl, hyperbare / rachi]").withFormats(
            new Format("DINJBUPI2A", "l, eq. 5 mg / ml base, 4 ml, amp [fr:i, éq. 5 mg / ml base, 4 ml, amp]", Unit.ML)
        ),
        new Drug("DINJCAFC", "CAFFEINE CITRATE [fr:CAFEINE CITRATE]").withFormats(
            new Format("DINJCAFC1A", "10 mg / ml, eq. 5 mg caffeine base, 1 ml, amp. [fr:10 mg / ml, éq. 5 mg caféine base, 1 ml, amp.]", Unit.ML),
            new Format("DINJCAFC2A", "20 mg / ml eq. 10 mg caffeine base, 1 ml, amp. [fr:20 mg / ml, éq. 10 mg caféine base, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJCALG", "CALCIUM GLUCONATE [fr:CALCIUM GLUCONATE]").withFormats(
            new Format("DINJCALG1A", "100 mg / ml, 10 ml, amp. [fr:100 mg / ml, 10 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJCAPR", "CAPREOMYCIN sulfate [fr:CAPREOMYCINE sulfate]").withFormats(
            new Format("DINJCAPR1V", "eq. 1 g base, powder, vial [fr:éq. 1 g base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCEFA", "CEFAZOLIN [fr:CEFAZOLINE]").withFormats(
            new Format("DINJCEFA1V", "1 g, (IV), powder, vial [fr:1 g, (IV), poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCEFL", "CEFTRIAXONE sodium + lidocaine IM [fr:CEFTRIAXONE sodique + lidocaine IM]").withFormats(
            new Format("DINJCEFL1V", "eq. 1 g base, powder, vial [fr:éq. 1 g base, poudre, fl.]", Unit.MG),
            new Format("DINJCEFL2V", "eq. 250 mg base, powd, vial [fr:éq. 250 mg base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCEFO", "CEFOTAXIME sodium [fr:CEFOTAXIME sodique]").withFormats(
            new Format("DINJCEFO2V", "eq. 250 mg base, vial [fr:éq. 250 mg base, fl.]", Unit.MG),
            new Format("DINJCEFO5V", "eq. 500 mg base, vial [fr:éq. 500 mg base, fl.]", Unit.MG)
        ),
        new Drug("DINJCEFT", "CEFTRIAXONE sodium [fr:CEFTRIAXONE sodique]").withFormats(
            new Format("DINJCEFT1V", "eq. 1 g base, powder, vial [fr:éq. 1 g base, poudre, fl.]", Unit.MG),
            new Format("DINJCEFT2V", "eq. 250 mg base, powder, vial [fr:éq. 250 mg base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCEFZ", "CEFTAZIDIME [fr:CEFTAZIDIME]").withFormats(
            new Format("DINJCEFZ1V", "1 g, powder, vial [fr:1 g, poudre, fl.]", Unit.MG),
            new Format("DINJCEFZ2V", "2 g, powder, vial [fr:2 g, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCHLO", "CHLORAMPHENICOL [fr:CHLORAMPHENICOL]").withFormats(
            new Format("DINJCHLO1V", "1 g powder, vial [fr:1 g, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJCIPR", "CIPROFLOXACIN salt [fr:sel de CIPROFLOXACINE]").withFormats(
            new Format("DINJCIPR2FBF", "eq. 2 mg / ml base, 100 ml, flex. bag PVC free [fr:éq. 2 mg / ml base, 100 ml, poche sple ssPVC]", Unit.ML),
            new Format("DINJCIPR2SRF", "eq. 2 mg / ml base, 100 ml, semi-r. bot PVCfree [fr:éq. 2 mg / ml base, 100 ml, fl. semi-r. ssPVC]", Unit.ML)
        ),
        new Drug("DINJCLIN", "CLINDAMYCIN phosphate [fr:CLINDAMYCINE phosphate]").withFormats(
            new Format("DINJCLIN3A", "eq. 150 mg base / ml, 2 ml, amp. [fr:éq. 150 mg base / ml, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJCLON", "CLONIDINE [fr:CLONIDINE]").withFormats(
            new Format("DINJCLON1A", "0.15 mg / ml, 1 ml, amp. [fr:0.15 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJCLOX", "CLOXACILLIN sodium [fr:CLOXACILLINE sodique]").withFormats(
            new Format("DINJCLOX5VV", "eq. 500 mg base, powder, vial IV [fr:éq. 500 mg base, poudre, fl. IV]", Unit.MG)
        ),
        new Drug("DINJCOLI", "COLISTIMETHATE sodium [fr:COLISTIMETHATE sodique]").withFormats(
            new Format("DINJCOLI1V", "1 MIU, powder, vial [fr:1 MUI, poudre, fl.]", Unit.MG),
            new Format("DINJCOLI2V", "2 M IU, powder, vial, for infusion [fr:2 M UI, poudre, flacon, pour perf.]", Unit.MG)
        ),
        new Drug("DINJCOTR", "COTRIMOXAZOLE [fr:COTRIMOXAZOLE]").withFormats(
            new Format("DINJCOTR4A", "80 mg / 16 mg / ml, 5 ml for infusion, amp. [fr:80 mg / 16 mg / ml, 5 ml pour perfusion, amp.]", Unit.ML)
        ),
        new Drug("DINJDEFE", "DEFEROXAMINE (desferrioxamine) mesilate [fr:DEFEROXAMINE (desferrioxamine) mesilate]").withFormats(
            new Format("DINJDEFE5V", "500 mg, powder, vial [fr:500 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJDEXA", "DEXAMETHASONE phosphate [fr:DEXAMETHASONE phosphate]").withFormats(
            new Format("DINJDEXA4A", "4 mg / ml, 1 ml, amp. [fr:4 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJDIAZ", "DIAZEPAM [fr:DIAZEPAM]").withFormats(
            new Format("DINJDIAZ1A", "5 mg / ml, 2 ml, amp. [fr:5 mg / ml, 2 ml, amp.]", Unit.ML),
            new Format("DINJDIAZ1AE", "5 mg / ml, 2 ml, emulsion, amp. [fr:5 mg / ml, 2 ml, émulsion, amp.]", Unit.ML)
        ),
        new Drug("DINJDICL", "DICLOFENAC sodium [fr:DICLOFENAC sodique]").withFormats(
            new Format("DINJDICL7A", "25 mg / ml, 3 ml, amp. [fr:25 mg / ml, 3 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJDIGO", "DIGOXIN [fr:DIGOXINE]").withFormats(
            new Format("DINJDIGO5A", "0.25 mg / ml, 2 ml, amp. [fr:0.25 mg / ml, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJDILT", "DILTIAZEM hydrochloride [fr:DILTIAZEM chlorhydrate]").withFormats(
            new Format("DINJDILT2V", "25 mg, powder, vial [fr:25 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJDOBU", "DOBUTAMINE HCl. [fr:DOBUTAMINE HCl.]").withFormats(
            new Format("DINJDOBU2A", "eq. 12.5 mg / ml base, 20 ml, sol for infusion [fr:éq. 12.5 mg / ml base, 20 ml, sol pour perfusion]", Unit.ML)
        ),
        new Drug("DINJDOPA", "DOPAMINE hydrochloride [fr:DOPAMINE chlorhydrate]").withFormats(
            new Format("DINJDOPA2A", "40 mg / ml, 5 ml, amp. [fr:40 mg / ml, 5 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJDOPL", "DOXORUBICIN HCl, pegylated liposomal [fr:DOXORUBICINE HCl, pégylée liposomale]").withFormats(
            new Format("DINJDOPL2V", "l, 2 mg / ml, 10 ml vial [fr:e, 2 mg / ml, 10 ml fl.]", Unit.ML),
            new Format("DINJDOPL5V", "l, 2 mg / ml, 25 ml vial [fr:e, 2 mg / ml, 25 ml fl.]", Unit.ML)
        ),
        new Drug("DINJDOXO", "DOXORUBICIN hydrochloride [fr:DOXORUBICINE chlorhydrate]").withFormats(
            new Format("DINJDOXO1V", "10 mg, powder, vial [fr:10 mg, poudre, fl.]", Unit.MG),
            new Format("DINJDOXO1V5", "2 mg / ml, 5 ml, vial [fr:2 mg / ml, 5 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJEFLO", "EFLORNITHINE hydrochloride [fr:EFLORNITHINE chlorhydrate]").withFormats(
            new Format("DINJEFLO2V", "eq. 200 mg / ml base, 100 ml, vial [fr:éq. 200 mg / ml base, 100 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJENOX", "ENOXAPARIN sodium [fr:ENOXAPARINE sodique]").withFormats(
            new Format("DINJENOX10S", "10.000 IU / 1 ml, syringe [fr:10000 UI / 1 ml, seringue]", Unit.ML),
            new Format("DINJENOX20S", "2.000 IU / 0.2 ml, syringe [fr:2000 UI / 0.2 ml, seringue]", Unit.ML),
            new Format("DINJENOX40S", "4.000 IU / 0.4 ml, syringe [fr:4000 UI / 0.4 ml, seringue]", Unit.ML),
            new Format("DINJENOX60S", "6.000 IU / 0.6 ml, syringe [fr:6000 UI / 0.6 ml, seringue]", Unit.ML)
        ),
        new Drug("DINJEPHE", "EPHEDRINE hydrochloride [fr:EPHEDRINE chlorhydrate]").withFormats(
            new Format("DINJEPHE3A", "30 mg / ml, 1 ml, amp. [fr:30 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJEPIN", "EPINEPHRINE (adrenaline) tartrate [fr:EPINEPHRINE (adrenaline) tartrate]").withFormats(
            new Format("DINJEPIN1AM", "eq. 1 mg / ml base, 1 ml amp IM [fr:éq. 1 mg / ml base, 1 ml amp IM]", Unit.ML),
            new Format("DINJEPIN1AV", "eq. 1 mg / ml base, 1 ml amp IV [fr:éq. 1 mg / ml base, 1 ml amp IV]", Unit.ML)
        ),
        new Drug("DINJEPOA", "EPOETIN ALFA [fr:EPOETINE ALFA]").withFormats(
            new Format("DINJEPOA1S", "10000 IU / ml, 1 ml, graduated syringe [fr:10000 UI / ml, 1 ml, seringue graduée]", Unit.ML)
        ),
        new Drug("DINJERYT", "ERYTHROMYCIN lactobionate [fr:ERYTHROMYCINE lactobionate]").withFormats(
            new Format("DINJERYT1V", "eq. to 1 g base, pdr, vial [fr:éq. à 1 g base, pdr, fl.]", Unit.MG)
        ),
        new Drug("DINJETAM", "ETAMSYLATE [fr:ETAMSYLATE]").withFormats(
            new Format("DINJETAM2A", "125 mg / ml, 2 ml, amp. [fr:125 mg / ml, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJETON", "ETONOGESTREL implant [fr:ETONOGESTREL implant]").withFormats(
            new Format("DINJETON6I", "1 x 68 mg, with applicator s. u. [fr:1 x 68 mg, avec applicateur u. u.]", Unit.MG)
        ),
        new Drug("DINJFENT", "FENTANYL citrate [fr:FENTANYL citrate]").withFormats(
            new Format("DINJFENT1A", "eq. 0.05 mg / ml base, 2 ml, amp. [fr:éq. 0.05 mg / ml base, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJFERC", "FERRIC carboxymaltose [fr:carboxymaltose FERRIQUE]").withFormats(
            new Format("DINJFERC1A", "eq. 50 mg / ml iron, 2 ml, amp. [fr:eq. 50 mg / ml fer, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJFLUC", "FLUCONAZOLE [fr:FLUCONAZOLE]").withFormats(
            new Format("DINJFLUC2FBF", "2 mg / ml, 100 ml, flexible bag PVC free [fr:2 mg / ml, 100 ml, poche souple sans PVC]", Unit.ML),
            new Format("DINJFLUC2SRF", "2 mg / ml, 100 ml, semi-rigid bot. PVC free [fr:2 mg / ml, 100 ml, fl. semi-rigide sans PVC]", Unit.ML)
        ),
        new Drug("DINJFLUM", "FLUMAZENIL [fr:FLUMAZENIL]").withFormats(
            new Format("DINJFLUM1A", "0.1 mg / ml, 10 ml, amp. [fr:0.1 mg / 1 ml, 10 ml, amp.]", Unit.ML),
            new Format("DINJFLUM5A", "0.1 mg / ml, 5 ml, amp. [fr:0.1 mg / ml, 5 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJFURO", "FUROSEMIDE [fr:FUROSEMIDE]").withFormats(
            new Format("DINJFURO2A", "10 mg / ml, 2 ml, amp. [fr:10 mg / ml, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJGANC", "GANCICLOVIR sodium [fr:GANCICLOVIR sodique]").withFormats(
            new Format("DINJGANC5V", "eq. 500 mg base, powder, vial fr infusion [fr:éq. 500 mg base, poudre, flacon perfusion]", Unit.MG)
        ),
        new Drug("DINJGENT", "GENTAMICIN sulfate [fr:GENTAMICINE sulfate]").withFormats(
            new Format("DINJGENT2A", "eq. 10 mg / ml base, 2 ml, amp. [fr:éq. 10 mg / ml base, 2 ml, amp.]", Unit.ML),
            new Format("DINJGENT2V", "eq. 10 mg / ml base, 2 ml, vial [fr:éq. 10 mg / ml base, 2 ml, fl.]", Unit.ML),
            new Format("DINJGENT8A", "eq. 40 mg / ml base, 2 ml, amp. [fr:eq. 40 mg / ml base, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJGLUC", "GLUCOSE [fr:GLUCOSE]").withFormats(
            new Format("DINJGLUC1A1", "hypertonic, 10%, 10 ml, amp [fr:hypertonique, 10%, 10 ml, amp]", Unit.ML),
            new Format("DINJGLUC3A2", "HYPER, 30%, 20 ml, amp. [fr:HYPERTONIQUE, 30%, 20 ml, amp.]", Unit.ML),
            new Format("DINJGLUC5V5", "hypertonic, 50%, 50 ml, vial [fr:hypertonique, 50%, 50 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJGLYC", "GLYCOPYRRONIUM bromide [fr:GLYCOPYRRONIUM bromure]").withFormats(
            new Format("DINJGLYC2A", "0.2 mg / ml, 1 ml, amp. [fr:0.2 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJHALP", "HALOPERIDOL [fr:HALOPERIDOL]").withFormats(
            new Format("DINJHALP5A", "5 mg / ml, 1 ml, amp. [fr:5 mg / ml, 1 ml, amp.]", Unit.ML),
            new Format("DINJHALP5AD", "decanoate, 50 mg / ml, 1 ml, amp. [fr:decanoate, 50 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJHEPA", "HEPARIN SODIUM [fr:HEPARINE SODIQUE]").withFormats(
            new Format("DINJHEPA2A", "5000 IU / ml, 5 ml, amp. [fr:5000 UI / ml, 5 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJHYDA", "HYDRALAZINE hydrochloride [fr:HYDRALAZINE chlorhydrate]").withFormats(
            new Format("DINJHYDA2A", "20 mg, powder, amp. [fr:20 mg, poudre, amp.]", Unit.MG)
        ),
        new Drug("DINJHYDR", "HYDROCORTISONE sodium succinate [fr:HYDROCORTISONE succinate sodique]").withFormats(
            new Format("DINJHYDR1V", "eq. 100 mg base, powder, vial [fr:eq. 100 mg base, poudre, fl]", Unit.MG),
            new Format("DINJHYDR1VS", "eq. 100 mg base, powder, vial +solvent [fr:éq. 100 mg base, fl. pdre + solvant]", Unit.MG)
        ),
        new Drug("DINJHYOS", "HYOSCINE BUTYLBROMIDE (scopolamine butylbrom) [fr:BUTYLBROMURE HYOSCINE (butylbrom. scopolamine)]").withFormats(
            new Format("DINJHYOS2A", "20 mg / 1 ml, amp [fr:20 mg / 1 ml, amp]", Unit.ML)
        ),
        new Drug("DINJIMCI", "IMIPENEM / CILASTATIN sodium [fr:IMIPENEME / CILASTATIN sodium]").withFormats(
            new Format("DINJIMCI55V", "500 mg / 500 mg, powder, vial [fr:500 mg / 500 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJINSA", "INSULIN [fr:INSULINE]").withFormats(
            new Format("DINJINSAB3APL", "LISPRO, BIPHASIC 25-75 IU / ml, 3 ml, autoinj. pref. L [fr:LISPRO, BIPHASIQUE 25-75 UI / ml, 3 ml, stylo prér. L]", Unit.ML),
            new Format("DINJINSAB3APN", "ASPART, BIPHASIC 30-70 IU / ml, 3 ml, autoinj. pref. N [fr:ASPART, BIPHASIQUE 30-70 UI / ml, 3 ml, stylo prér. N]", Unit.ML),
            new Format("DINJINSAL1VS", "GLARGINE, LONG 100 IU / ml, 10 ml, vial S [fr:GLARGINE, LENTE 100 UI / ml, 10 ml, fl. S]", Unit.ML),
            new Format("DINJINSAL3APS", "GLARGINE, LONG, 100 IU / ml, 3 ml, autoinjector pref. S [fr:GLARGINE, LENTE, 100 UI / ml, 3 ml, stylo prérempli S]", Unit.ML),
            new Format("DINJINSAU3APL", "LISPRO, ULTRARAPID 100 UI / ml, 3 ml, autoinject. pref. L [fr:LISPRO, ULTRARAPIDE 100 UI / ml, 3 ml, stylo prér. L]", Unit.ML),
            new Format("DINJINSAU3APN", "ASPART, ULTRARAPID 100 UI / ml, 3 ml, autoinject. pref. N [fr:ASPART, ULTRARAPIDE 100 UI / ml, 3 ml, stylo prér. N]", Unit.ML)
        ),
        new Drug("DINJINSH", "INSULIN HUMAN [fr:INSULINE HUMAINE]").withFormats(
            new Format("DINJINSHB1VL", "BIPHASIC 30-70 IU / ml, 10 ml, vial L [fr:BIPHASIQUE 30-70 UI / ml, 10 ml, fl. L]", Unit.ML),
            new Format("DINJINSHB1VN", "BIPHASIC 30-70 IU / ml, 10 ml, vial N [fr:BIPHASIQUE 30-70 UI / ml, 10 ml, fl. N]", Unit.ML),
            new Format("DINJINSHB1VS", "BIPHASIC 30-70 IU / ml, 10 ml, vial S [fr:BIPHASIQUE 30-70 UI / ml, 10 ml, fl. S]", Unit.ML),
            new Format("DINJINSHI1VN", "ISOPHANE (NPH) 100 UI / ml, 10 ml, vial N [fr:ISOPHANE (NPH) 100 UI / ml, 10 ml, fl. N]", Unit.ML),
            new Format("DINJINSHI1VS", "ISOPHANE (NPH) 100 UI / ml, 10 ml, vial S [fr:ISOPHANE (NPH) 100 UI / ml, 10 ml, fl. S]", Unit.ML),
            new Format("DINJINSHI3APN", "ISOPHANE (NPH) 100 UI / ml, 3 ml, autoinj. pref. N [fr:ISOPHANE (NPH) 100 UI / ml, 3 ml, stylo prér. N]", Unit.ML),
            new Format("DINJINSHR1VN", "RAPID 100 IU / ml, 10 ml, vial N [fr:RAPIDE 100 UI / ml, 10 ml, fl. N]", Unit.ML),
            new Format("DINJINSHR1VS", "RAPID 100 IU / ml, 10 ml, vial S [fr:RAPIDE 100 UI / ml, 10 ml, fl. S]", Unit.ML)
        ),
        new Drug("DINJISOB", "ISOSORBIDE DINITRATE [fr:ISOSORBIDE DINITRATE]").withFormats(
            new Format("DINJISOB1A", "1 mg / ml, 10 ml, amp. [fr:1 mg / ml, 10 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJKANA", "KANAMYCIN sulfate [fr:KANAMYCINE sulfate]").withFormats(
            new Format("DINJKANA1A4", "eq. 0.250 g / ml base, 4 ml, amp. [fr:éq. 0.250 g / ml base, 4 ml, amp.]", Unit.ML),
            new Format("DINJKANA1V", "eq. 1 g base, powder, vial [fr:éq. 1 g base, poudre, fl.]", Unit.MG),
            new Format("DINJKANA5A2", "eq. 0.250 g / ml base, 2 ml, amp. [fr:éq. 0.250 g / ml base, 2 ml, amp.]", Unit.ML),
            new Format("DINJKANA5V", "eq. 0.5 g base, powder, vial [fr:éq. 0.5 g base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJKETA", "KETAMINE hydrochloride [fr:KETAMINE chlorhydrate]").withFormats(
            new Format("DINJKETA2A", "eq. 50 mg / ml base, 5 ml, amp. [fr:éq. 50 mg / ml base, 5 ml, amp.]", Unit.ML),
            new Format("DINJKETA5V", "eq. 50 mg / ml base, 10 ml, vial [fr:éq. 50 mg / ml base, 10 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJLABE", "LABETALOL hydrochloride [fr:LABETALOL chlorhydrate]").withFormats(
            new Format("DINJLABE1A", "5 mg / ml, 20 ml amp. [fr:5 mg / ml, 20 ml amp.]", Unit.ML)
        ),
        new Drug("DINJLEVB", "LEVOBUPIVACAINE hydrochloride [fr:LEVOBUPIVACAINE chlorhydrate]").withFormats(
            new Format("DINJLEVB2A", "eq. 2.5 mg / ml base, 10 ml, amp. [fr:éq. 2.5 mg / ml base, 10 ml, amp.]", Unit.ML),
            new Format("DINJLEVB5A", "eq. 5 mg / ml base, 10 ml, amp [fr:éq. 5 mg / ml base, 10 ml, amp]", Unit.ML)
        ),
        new Drug("DINJLEVE", "LEVETIRACETAM [fr:LEVETIRACETAM]").withFormats(
            new Format("DINJLEVE5V", "100 mg / ml, 5 ml, vial [fr:100 mg / ml, 5 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJLEVN", "LEVONORGESTREL implant [fr:LEVONORGESTREL implant]").withFormats(
            new Format("DINJLEVN15I", "2 x 75 mg (Jadelle) + trocar [fr:2 x 75 mg (Jadelle) + trocart]", Unit.MG)
        ),
        new Drug("DINJLIDE", "LIDOCAINE / EPINEPHRINE [fr:LIDOCAINE / EPINEPHRINE]").withFormats(
            new Format("DINJLIDE1C2", "1% / 1/200.000, 20 ml, vial [fr:1% / 1/200000, 20 ml, fl.]", Unit.ML),
            new Format("DINJLIDE2C1", "2% / 1/80000, 1.8 ml, cart. [fr:2% / 1/80000, 1.8 ml, cart.]", Unit.ML)
        ),
        new Drug("DINJLIDO", "LIDOCAINE hydrochloride [fr:LIDOCAINE chlorhydrate]").withFormats(
            new Format("DINJLIDO1A1", "1%, preservative-free, 10 ml, amp [fr:1%, sans conservateur, 10 ml, amp]", Unit.ML),
            new Format("DINJLIDO1A5", "1%, preservative-free, 5 ml, plast. amp [fr:1%, sans conservateur, 5 ml, amp. plast]", Unit.ML),
            new Format("DINJLIDO1V2", "1%, preservative-free, 20 ml, vial [fr:1%, sans conservateur, 20 ml, fl.]", Unit.ML),
            new Format("DINJLIDO2V2", "2%, preservative-free, 20 ml, vial [fr:2%, sans conservateur, 20 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJLIPE", "LIPID emulsion [fr:émulsion LIPIDIQUE]").withFormats(
            new Format("DINJLIPE2FBF2", "20%, 250 ml, flex. bag, PVC free [fr:20%, 250 ml bot., poche souple, sans PVC]", Unit.ML)
        ),
        new Drug("DINJMAGS", "MAGNESIUM sulfate [fr:MAGNESIUM sulfate]").withFormats(
            new Format("DINJMAGS5A", "0.5 g / ml, 10 ml, amp. [fr:0.5 g / ml, 10 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJMEDR", "MEDROXYPROGESTERONE acetate [fr:MEDROXYPROGESTERONE acétate]").withFormats(
            new Format("DINJMEDR1S", "150 mg, 1 ml, syringe [fr:150 mg, 1 ml, seringue]", Unit.ML),
            new Format("DINJMEDR1V", "150 mg, 1 ml, vial [fr:150 mg, 1 ml, fl.]", Unit.ML),
            new Format("DINJMEDR6IP", "104 mg / 0.65 ml, injector prefilled [fr:104 mg / 0.65 ml, injecteur prérempl]", Unit.ML)
        ),
        new Drug("DINJMEGA", "MEGLUMINE ANTIMONIATE [fr:MEGLUMINE ANTIMONIATE]").withFormats(
            new Format("DINJMEGA4A", "pentaval. antimony 81 mg / ml, 5 ml, amp [fr:antimoine pentaval. 81 mg / ml, 5 ml, amp]", Unit.ML)
        ),
        new Drug("DINJMELA", "MELARSOPROL [fr:MELARSOPROL]").withFormats(
            new Format("DINJMELA3A5", "36 mg / ml, 5 ml, amp. [fr:36 mg / ml, 5 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJMERG", "METHYLERGOMETRINE maleate [fr:METHYLERGOMETRINE maleate]").withFormats(
            new Format("DINJMERG2A", "0.2 mg / ml, 1 ml, amp. [fr:0.2 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJMERO", "MEROPENEM [fr:MEROPENEME]").withFormats(
            new Format("DINJMERO1V", "1 g, powder, vial [fr:1 g, poudre, fl]", Unit.MG),
            new Format("DINJMERO5V", "500 mg, powder, vial [fr:500 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJMETN", "METRONIDAZOLE [fr:METRONIDAZOLE]").withFormats(
            new Format("DINJMETN5FBF", "5 mg / ml, 100 ml, flex. bag PVC free [fr:5 mg / ml, 100 ml, poche souple sans PVC]", Unit.ML),
            new Format("DINJMETN5SRF", "5 mg / ml, 100 ml, semi-rigid bot. PVC free [fr:5 mg / ml, 100 ml, fl. semi-rigide sans PVC]", Unit.ML)
        ),
        new Drug("DINJMETO", "METOCLOPRAMIDE hydrochloride [fr:METOCLOPRAMIDE chlorhydrate]").withFormats(
            new Format("DINJMETO1A", "5 mg / ml, 2 ml, amp. [fr:5 mg / ml, 2 ml, amp]", Unit.ML)
        ),
        new Drug("DINJMIDA", "MIDAZOLAM [fr:MIDAZOLAM]").withFormats(
            new Format("DINJMIDA5A", "1 mg / ml, 5 ml, amp [fr:1 mg / ml, 5 ml, amp]", Unit.ML)
        ),
        new Drug("DINJMORP", "MORPHINE hydrochloride [fr:MORPHINE chlorhydrate]").withFormats(
            new Format("DINJMORP1A", "10 mg / ml, 1 ml, amp. [fr:10 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJNADR", "NADROPARIN calcium [fr:NADROPARINE calcique]").withFormats(
            new Format("DINJNADR2S", "1900 IU / 0.2 ml, syringe [fr:1900 UI / 0.2 ml, seringue]", Unit.ML),
            new Format("DINJNADR3S", "2850 IU / 0.3 ml, syringe [fr:2850 UI / 0.3 ml, seringue]", Unit.ML),
            new Format("DINJNADR4S", "3800 IU / 0.4 ml, syringe [fr:3800 UI / 0.4 ml, seringue]", Unit.ML),
            new Format("DINJNADR5S", "5700 UI / 0.6 ml, syringe [fr:5700 UI / 0.6 ml, seringue]", Unit.ML)
        ),
        new Drug("DINJNALO", "NALOXONE hydrochloride [fr:NALOXONE chlorhydrate]").withFormats(
            new Format("DINJNALO4A", "0.4 mg / ml, 1 ml, amp. [fr:0.4 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJNEOS", "NEOSTIGMINE methylsulfate [fr:NEOSTIGMINE méthylsulfate]").withFormats(
            new Format("DINJNEOS2A", "2.5 mg / ml, 1 ml, amp. [fr:2.5 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJNEPI", "NOREPINEPHRINE (noradrenaline) tartrate [fr:NOREPINEPHRINE (noradrénaline) tartrate]").withFormats(
            new Format("DINJNEPI4AV", "eq. 1 mg / ml base, 4 ml [fr:éq. 1 mg / ml base, 4 ml]", Unit.ML)
        ),
        new Drug("DINJNICA", "NICARDIPINE hydrochloride [fr:NICARDIPINE chlorhydrate]").withFormats(
            new Format("DINJNICA1A", "1 mg / ml, 10 ml, amp. [fr:1 mg / ml, 10 ml, amp.]", Unit.ML),
            new Format("DINJNICA5A", "1 mg / 1 ml, 5 ml, amp. [fr:1 mg / 1 ml, 5 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJOMEP", "OMEPRAZOLE sodium [fr:OMEPRAZOLE sodique]").withFormats(
            new Format("DINJOMEP4V", "eq. 40 mg base, powder, vial, fr infusion [fr:éq. 40 mg base, poudre, fl. pr perfusion]", Unit.MG)
        ),
        new Drug("DINJONDA", "ONDANSETRON hydrochloride [fr:ONDANSETRON chlorhydrate]").withFormats(
            new Format("DINJONDA4A", "eq. 2 mg / ml base, 2 ml, amp. [fr:éq. 2 mg / ml base, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJOXYT", "OXYTOCIN [fr:OXYTOCINE]").withFormats(
            new Format("DINJOXYT1A", "10 IU / ml, 1 ml, amp. [fr:10 UI / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJPACL", "PACLITAXEL [fr:PACLITAXEL]").withFormats(
            new Format("DINJPACL1V", "6 mg / ml sol. to be diluted, 16.7 ml, bot. [fr:6 mg / ml sol. a diluer, 16.7 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJPARA", "PARACETAMOL (acetaminophen) [fr:PARACETAMOL (acétaminophène)]").withFormats(
            new Format("DINJPARA1B", "10 mg / ml, 100 ml, bot. [fr:10 mg / ml, 100 ml, fl.]", Unit.ML),
            new Format("DINJPARA1FBF", "10 mg / ml, 100 ml, flex. bag PVC free [fr:10 mg / ml, 100 ml, poche s. ss PVC]", Unit.ML),
            new Format("DINJPARA5B", "10 mg / ml, 50 ml, bot. [fr:10 mg / ml, 50 ml, fl.]", Unit.ML),
            new Format("DINJPARA5FBF", "10 mg / ml, 50 ml, flex. bag PVC free [fr:10 mg / ml, 50 ml, poche s. ss PVC]", Unit.ML)
        ),
        new Drug("DINJPARO", "PAROMOMYCIN sulfate [fr:PAROMOMYCINE sulfate]").withFormats(
            new Format("DINJPARO1A", "eq. 375 mg / ml base, 2 ml, amp [fr:éq. 375 mg / ml base, 2 ml, amp]", Unit.ML)
        ),
        new Drug("DINJPENB", "BENZATHINE BENZYLPENICILLIN [fr:BENZATHINE BENZYLPENICILLINE]").withFormats(
            new Format("DINJPENB1V", "1.2 M IU, powder, vial [fr:1.2 M UI, poudre, fl.]", Unit.MG),
            new Format("DINJPENB1VS", "1.2 M IU, powder, vial+ solvent [fr:1.2 M UI, poudre, fl. +solvant]", Unit.MG),
            new Format("DINJPENB2V", "2.4 M IU, powder, vial [fr:2.4 M UI, poudre, fl.]", Unit.MG),
            new Format("DINJPENB2VS", "2.4 M IU, powder, vial+ solvent [fr:2.4 M UI, poudre, fl. +solvant]", Unit.MG)
        ),
        new Drug("DINJPENG", "BENZYLPENICILLIN (peni G, crystal peni) [fr:BENZYLPENICILLINE (peni G, cristal peni)]").withFormats(
            new Format("DINJPENG1V", "), 1 MIU, powder, vial [fr:), 1 MUI, poudre, fl]", Unit.MG),
            new Format("DINJPENG5V", "), 5 MIU, powder, vial [fr:), 5 MUI, poudre, fl]", Unit.MG)
        ),
        new Drug("DINJPENT", "PENTAMIDINE isetionate [fr:PENTAMIDINE isetionate]").withFormats(
            new Format("DINJPENT3V", "300 mg, powder, vial [fr:300 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJPHEE", "PHENYLEPHRINE hydrochloride [fr:PHENYLEPHRINE chlorhydrate]").withFormats(
            new Format("DINJPHEE5A", "eq. 50 µg base / ml, 10 ml amp. [fr:éq. 50 µg base / ml, 10 ml amp.]", Unit.ML)
        ),
        new Drug("DINJPHEN", "PHENOBARBITAL sodium [fr:PHENOBARBITAL sodique]").withFormats(
            new Format("DINJPHEN2A1", "200 mg / ml, 1 ml, amp. [fr:200 mg / ml, 1 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJPHEY", "PHENYTOIN sodium [fr:PHENYTOINE sodique]").withFormats(
            new Format("DINJPHEY2A", "50 mg / ml, 5 ml, amp. [fr:50 mg / ml, 5 ml, amp.]", Unit.ML),
            new Format("DINJPHEY2V", "50 mg / ml, 5 ml, vial [fr:50 mg / ml, 5 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJPHLT", "PHLOROGLUCINOL / TRIMETHYLPHLOROGLUCINOL [fr:PHLOROGLUCINOL / TRIMETHYLPHLOROGLUCINOL]").withFormats(
            new Format("DINJPHLT44A", "10 mg / ml / 10 µg / ml, 4 ml, amp. [fr:10 mg / ml / 10 µg / ml, 4 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJPHYT", "PHYTOMENADIONE (vitamin K1) [fr:PHYTOMENADIONE (vitamine K1)]").withFormats(
            new Format("DINJPHYT2AN", "10 mg / ml (2 mg / 0.2 ml), 0.2 ml amp. [fr:10 mg / ml (2 mg / 0.2 ml), 0.2 ml amp.]", Unit.ML)
        ),
        new Drug("DINJPITA", "PIPERACILLIN / TAZOBACTAM [fr:PIPERACILLINE / TAZOBACTAM]").withFormats(
            new Format("DINJPITA45V", "4 g / 500 mg, powder, vial for inf. [fr:4 g / 500 mg, poudre, fl. pour perf.]", Unit.MG)
        ),
        new Drug("DINJPOTC", "POTASSIUM chloride [fr:POTASSIUM chlorure]").withFormats(
            new Format("DINJPOTC1A", "100 mg / ml, 10 ml, amp. [fr:100 mg / ml, 10 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJPRAL", "PRALIDOXIME [fr:PRALIDOXIME]").withFormats(
            new Format("DINJPRAL2A1S", "2%, vial powder + amp. solvant, 10 ml. [fr:2% flacons poudre+ ampoules solvant, 10 ml.]", Unit.ML)
        ),
        new Drug("DINJPROM", "PROMETHAZINE hydrochloride [fr:PROMETHAZINE chlorhydrate]").withFormats(
            new Format("DINJPROM2A", "eq. 25 mg / ml base, 1 ml, amp. [fr:éq. 25 mg / ml base, 1 ml, amp.]", Unit.ML),
            new Format("DINJPROM5A", "eq. 25 mg / ml base, 2 ml, amp. [fr:éq. 25 mg / ml base, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJPROP", "PROPOFOL [fr:PROPOFOL]").withFormats(
            new Format("DINJPROP2AE", "10 mg / ml, 20 ml, emulsion, amp. [fr:10 mg / ml, 20 ml, émulsion, amp.]", Unit.ML)
        ),
        new Drug("DINJPROT", "PROTAMINE sulfate [fr:PROTAMINE sulfate]").withFormats(
            new Format("DINJPROT5A", "10 mg / ml, 5 ml, amp. [fr:10 mg / ml, 5 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJRANI", "RANITIDINE [fr:RANITIDINE]").withFormats(
            new Format("DINJRANI5A", "25 mg / ml, 2 ml, amp. [fr:25 mg / ml, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJRIBA", "RIBAVIRIN [fr:RIBAVIRINE]").withFormats(
            new Format("DINJRIBA1A", "100 mg / ml, 12 ml, amp. [fr:100 mg / ml, 12 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJRIFA", "RIFAMPICIN (R) [fr:RIFAMPICINE (R)]").withFormats(
            new Format("DINJRIFA6VS", "600 mg, powder, vial + solvent [fr:600 mg, poudre, fl. + solvant]", Unit.MG)
        ),
        new Drug("DINJSODB", "SODIUM BICARBONATE [fr:SODIUM BICARBONATE]").withFormats(
            new Format("DINJSODB8A1", "8.4%, 1 mEq / ml, 10 ml, amp. [fr:8.4%, 1 mEq / ml, 10 ml, amp.]", Unit.ML),
            new Format("DINJSODB8A2", "8.4%, 1 mEq / ml, 20 ml, amp. [fr:8.4%, 1 mEq / ml, 20 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJSODC", "SODIUM chloride [fr:SODIUM chlorure]").withFormats(
            new Format("DINJSODC1A1", "10%, 10 ml, amp. [fr:10%, 10 ml, amp.]", Unit.ML),
            new Format("DINJSODC2A1", "hypertonic, 20%, 10 ml, amp. [fr:hypertonique, 20%, 10 ml, amp.]", Unit.ML),
            new Format("DINJSODC9A1", "0.9%, 10 ml, amp. [fr:0.9%, 10 ml, amp.]", Unit.ML),
            new Format("DINJSODC9A5", "0.9%, 5 ml, plastic amp. [fr:0.9%, 5 ml, amp.]", Unit.ML),
            new Format("DINJSODC9AP1", "0.9%, 10 ml, plastic amp. [fr:0.9%, 10 ml, amp. plastique]", Unit.ML)
        ),
        new Drug("DINJSPEC", "SPECTINOMYCIN hydrochloride [fr:SPECTINOMYCINE chlorhydrate]").withFormats(
            new Format("DINJSPEC2V", "eq. 2 g base, powder, vial [fr:éq. 2 g base, poudre, fl.]", Unit.MG),
            new Format("DINJSPEC2VS", "eq. 2 g base, powder, vial+SOLVENT [fr:éq. 2 g base, poudre, fl. + SOLVANT]", Unit.MG)
        ),
        new Drug("DINJSSGL", "SODIUM STIBOGLUCONATE, pentaval. antimony [fr:SODIUM STIBOGLUCONATE, antimoine pentaval.]").withFormats(
            new Format("DINJSSGL1V1", "y 100 mg / ml 100 ml vial [fr:. 100 mg / ml 100 ml fl]", Unit.ML),
            new Format("DINJSSGL1V3", "y 100 mg / ml, 30 ml vial [fr:. 100 mg / ml, 30 ml fl]", Unit.ML)
        ),
        new Drug("DINJSTRE", "STREPTOMYCIN sulfate [fr:STREPTOMYCINE sulfate]").withFormats(
            new Format("DINJSTRE1V", "eq. 1 g base, powder, vial [fr:éq. 1 g base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJSTRK", "STREPTOKINASE [fr:STREPTOKINASE]").withFormats(
            new Format("DINJSTRK1V", "1.500.000 IU, powder, vial [fr:1.500.000 IU, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJSUXC", "SUXAMETHONIUM chloride [fr:SUXAMETHONIUM chlorure]").withFormats(
            new Format("DINJSUXC1A", "50 mg / ml, 2 ml, amp. [fr:50 mg / ml, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJTHIA", "THIAMINE (vitamin B1) [fr:THIAMINE (vitamine B1)]").withFormats(
            new Format("DINJTHIA1A", "50 mg / ml, 2 ml, amp. [fr:50 mg / ml, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJTHIO", "THIOPENTAL sodium [fr:THIOPENTAL sodique]").withFormats(
            new Format("DINJTHIO5V", "500 mg, powder, vial [fr:500 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJTRAM", "TRAMADOL hydrochloride [fr:TRAMADOL chlorhydrate]").withFormats(
            new Format("DINJTRAM1A", "50 mg / ml, 2 ml, amp. [fr:50 mg / ml, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJTRAN", "TRANEXAMIC ACID [fr:ACIDE TRANEXAMIQUE]").withFormats(
            new Format("DINJTRAN5A", "100 mg / ml, 5 ml amp. [fr:100 mg / ml, 5 ml amp.]", Unit.ML)
        ),
        new Drug("DINJUROK", "UROKINASE [fr:UROKINASE]").withFormats(
            new Format("DINJUROK1V", "100000 IU, powder, vial [fr:100000 UI, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJVALP", "VALPROATE SODIUM [fr:VALPROATE DE SODIUM]").withFormats(
            new Format("DINJVALP4A", "100 mg / ml, 4 ml amp. [fr:100 mg / ml, 4 ml amp.]", Unit.ML)
        ),
        new Drug("DINJVANC", "VANCOMYCIN hydrocloride [fr:VANCOMYCINE]").withFormats(
            new Format("DINJVANC1V", "eq. 1 g base, powder, vial [fr:chlorydrate, éq. 1 g base, poudre, fl.]", Unit.MG),
            new Format("DINJVANC5V", "eq. 500 mg base, powder, vial [fr:chlorhydrate, éq. 500 mg base, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJVECB", "VECURONIUM bromide [fr:VECURONIUM bromure]").withFormats(
            new Format("DINJVECB1V", "10 mg, powder, vial [fr:10 mg, poudre, fl.]", Unit.MG)
        ),
        new Drug("DINJVERA", "VERAPAMIL hydrochloride [fr:VERAPAMIL chlorhydrate]").withFormats(
            new Format("DINJVERA2A", "2.5 mg / ml, 2 ml, amp. [fr:2.5 mg / ml, 2 ml, amp.]", Unit.ML)
        ),
        new Drug("DINJVINC", "VINCRISTINE sulfate [fr:VINCRISTINE sulfate]").withFormats(
            new Format("DINJVINC1V", "1 mg / ml, 1 ml, vial [fr:1 mg / ml, 1 ml, fl.]", Unit.ML),
            new Format("DINJVINC2V", "1 mg / ml, 2 ml, vial [fr:1 mg / ml, 2 ml, fl.]", Unit.ML)
        ),
        new Drug("DINJWATE", "WATER for injection [fr:EAU pour injection]").withFormats(
            new Format("DINJWATE1A", "10 ml, plastic amp. [fr:10 ml, amp. plastique]", Unit.ML)
        )
    );

    Category PERFUSION = new Category("DINF", "perfusion", true).withDrugs(
        new Drug("DINFDERI", "DEXTROSE (GLUCOSE) / RINGER LACTATE [fr:GLUCOSE / RINGER LACTATE]").withFormats(
            new Format("DINFDERI5FBF5", "GLUCOSE 5% + RL, 500 ml, flex. bag, PVC free [fr:GLUCOSE 5% + RL, 500 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDERIXX10", "GLUCOSE 10% + RL, prepared by hand (nonstandard) [fr:GLUCOSE 10% + RL, préparé à la main (non standard)]", Unit.ML)
        ),
        new Drug("DINFDEXT", "DEXTROSE (GLUCOSE) [fr:GLUCOSE]").withFormats(
            new Format("DINFDEXT1FBF2", "10%, 250 ml, flex. bag, PVC free [fr:10%, 250 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDEXT1FBF5", "10%, 500 ml, flex. bag, PVC free [fr:10%, 500 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDEXT1FBP5", "10%, 500 ml, flex. bag, PVC [fr:10%, 500 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFDEXT1SRF2", "10%, 250 ml, semi-rigid bot., PVC free [fr:10%, 250 ml, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFDEXT1SRF5", "10%, 500 ml, semi-rigid bot., PVC free [fr:10%, 500 ml, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFDEXT5FBF1", "5%, 1 l, flex. bag, PVC free [fr:5%, 1 l, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDEXT5FBF2", "5%, 250 ml, flex. bag, PVC free [fr:5%, 250 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDEXT5FBF5", "5%, 500 ml, flex. bag, PVC free [fr:5%, 500 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFDEXT5FBP1", "5%, 1 l, flex. bag, PVC [fr:5%, 1 l, poche souple, PVC]", Unit.ML),
            new Format("DINFDEXT5FBP5", "5%, 500 ml, flex. bag, PVC [fr:5%, 500 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFDEXT5SRF1", "5%, 1 l, semi-rigid bot., PVC free [fr:5%, 1 l, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFDEXT5SRF5", "5%, 500 ml, semi-rigid bot., PVC free [fr:5%, 500 ml, fl. semi-rigide, sans PVC]", Unit.ML)
        ),
        new Drug("DINFMANN", "MANNITOL [fr:MANNITOL]").withFormats(
            new Format("DINFMANN2B5", "20%, 500 ml, bot. [fr:20%, 500 ml, fl.]", Unit.ML),
            new Format("DINFMANN2FBF5", "20%, 500 ml, flex. bag, PVC free [fr:20%, 500 ml, poche souple, sans PVC]", Unit.ML)
        ),
        new Drug("DINFPLAS", "MODIFIED FLUID GELATIN / POLYGELIN [fr:PLASMA SUBSTITUT, gélatine]").withFormats(
            new Format("DINFPLAS1FBF5", "500 ml, flex. bag, PVC free [fr:e, 500 ml, poche souple, ss PVC]", Unit.ML),
            new Format("DINFPLAS1FBP5", "500 ml, flex. bag, PVC [fr:e, 500 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFPLAS1SRF5", "500 ml, semi-rigid bt, PVCfree [fr:e, 500 ml, fl. semi-rigide, ss PVC]", Unit.ML)
        ),
        new Drug("DINFBLDT", "BLOOD transfusion [fr:transfusion SANGUINE]").withFormats(
            new Format("DINFBLDTON", "group O− [fr:groupe O−]", Unit.ML),
            new Format("DINFBLDTOP", "group O+ [fr:groupe O+]", Unit.ML),
            new Format("DINFBLDTAN", "group A− [fr:groupe A−]", Unit.ML),
            new Format("DINFBLDTAP", "group A+ [fr:groupe A+]", Unit.ML),
            new Format("DINFBLDTBN", "group B− [fr:groupe B−]", Unit.ML),
            new Format("DINFBLDTBP", "group B+ [fr:groupe B+]", Unit.ML),
            new Format("DINFBLDTABN", "group AB− [fr:groupe AB−]", Unit.ML),
            new Format("DINFBLDTABP", "group AB+ [fr:groupe AB+]", Unit.ML)
        ),
        new Drug("DINFRINL", "RINGER lactate [fr:RINGER lactate]").withFormats(
            new Format("DINFRINL1FBF1", "1 l, flex. bag, PVC free [fr:1 l, poche souple, sans PVC]", Unit.MG),
            new Format("DINFRINL1FBF5", "500 ml, flex. bag, PVC free [fr:500 m l, poche souple, sans PVC]", Unit.ML),
            new Format("DINFRINL1FBP1", "1 l, flex. bag, PVC [fr:1 l, poche souple, PVC]", Unit.MG),
            new Format("DINFRINL1FBP5", "500 ml, flex. bag, PVC [fr:500 m l, poche souple, PVC]", Unit.ML),
            new Format("DINFRINL1SRF1", "1 l, semi-rigid bot., PVC free [fr:1 l, fl. semi-rigide, sans PVC]", Unit.MG),
            new Format("DINFRINL1SRF5", "500 ml, semi-rigid bot., PVC free [fr:500 ml, fl. semi-rigide, sans PVC]", Unit.ML)
        ),
        new Drug("DINFSODC", "SODIUM chloride (NaCl) [fr:SODIUM chlorure (NaCl)]").withFormats(
            new Format("DINFSODC9FBF0", "0.9%, 100 ml, flex. bag, PVC free [fr:0.9%, 100 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFSODC9FBF1", "0.9%, 1 l, flex. bag, PVC free [fr:0.9%, 1 l, poche souple, sans PVC]", Unit.ML),
            new Format("DINFSODC9FBF2", "0.9%, 250 ml, flex. bag, PVC free [fr:0.9%, 250 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFSODC9FBF5", "0.9%, 500 ml, flex. bag, PVC free [fr:0.9%, 500 ml, poche souple, sans PVC]", Unit.ML),
            new Format("DINFSODC9FBP0", "0.9%, 100 ml, flex. bag, PVC [fr:0.9%, 100 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFSODC9FBP1", "0.9%, 1 l, flex. bag, PVC [fr:0.9%, 1 l, poche souple, PVC]", Unit.ML),
            new Format("DINFSODC9FBP2", "0.9%, 250 ml, flex. bag, PVC [fr:0.9%, 250 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFSODC9FBP5", "0.9%, 500 ml, flex. bag, PVC [fr:0.9%, 500 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFSODC9SRF0", "0.9%, 100 ml, semi-rigid bot., PVC free [fr:0.9%, 100 ml, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFSODC9SRF1", "0.9%, 1 l, semi-rigid bot., PVC free [fr:0.9%, 1 l, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFSODC9SRF2", "0.9%, 250 ml, semi-rigid bot., PVC free [fr:0.9%, 250 ml, fl. semi-rigide, sans PVC]", Unit.ML),
            new Format("DINFSODC9SRF5", "0.9%, 500 ml, semi-rigid bot., PVC free [fr:0.9%, 500 ml, fl. semi-rigide, sans PVC]", Unit.ML)
        ),
        new Drug("DINFWATE", "WATER FOR INJECTION [fr:EAU POUR PREPARATION INJECTABLE]").withFormats(
            new Format("DINFWATE1B1", "100 ml, bot. [fr:100 ml, fl.]", Unit.ML),
            new Format("DINFWATE1FB05", "50 ml, flex. bag, PVC free [fr:50 ml, poche souple, ss PVC]", Unit.ML),
            new Format("DINFWATE1FBF1", "100 ml, flex. bag, PVC free [fr:100 ml, poche souple, ss PVC]", Unit.ML),
            new Format("DINFWATE1FBF2", "250 ml, flex. bag, PVC free [fr:250 ml, poche souple, ss PVC]", Unit.ML),
            new Format("DINFWATE1FBP1", "100 ml, flex. bag, PVC [fr:100 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFWATE1FBP2", "250 ml, flex. bag, PVC [fr:250 ml, poche souple, PVC]", Unit.ML),
            new Format("DINFWATE1SRF1", "100 ml, semi-rigid bot., PVC free [fr:100 ml, fl. semi-rigide, ss PVC]", Unit.ML),
            new Format("DINFWATE1SRF2", "250 ml, semi-rigid bot., PVC free [fr:250 ml, fl. semi-rigide, ss PVC]", Unit.ML)
        )
    );

    Category EXTERNAL = new Category("DEXT", "external", false, UNSPECIFIED, OC).withDrugs(
        new Drug("DEXOACIV", "ACICLOVIR ophth. [fr:ACICLOVIR ophth.]").withFormats(
            new Format("DEXOACIV3T4", "3%, eye ointment, sterile, 4.5 g, tube [fr:3%, pommade ophtalmique, stérile, 4.5 g, tube]", Unit.ML)
        ),
        new Drug("DEXOATRO", "ATROPINE sulfate [fr:ATROPINE sulfate]").withFormats(
            new Format("DEXOATRO1D4", "1%, eye drops, ster, 0.4 ml, unidose, amp. [fr:1%, collyre, stér, 0.4 ml, unidose, amp.]", Unit.ML)
        ),
        new Drug("DEXOCHLO", "CHLORAMPHENICOL [fr:CHLORAMPHENICOL]").withFormats(
            new Format("DEXOCHLO5D1", "0.5%, eye drops, sterile, 10 ml, bot. [fr:0.5%, collyre, stérile, 10 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXODENP", "DEXAMETHASONE / NEOMYCIN / POLYMYXIN B [fr:DEXAMETHASONE / NEOMYCINE / POLYMYXINE B]").withFormats(
            new Format("DEXODENP513D5", "DEXAMETHASONE5 mg / NEOMYCIN17500 IU / 30000 IU, eye drops, 5 ml [fr:DEXAMETHASONE5 mg / NEOMYCINE17500 UI / 30000 UI, collyre, 5 ml]", Unit.ML)
        ),
        new Drug("DEXODEXN", "DEXAMETHASONE / NEOMYCIN [fr:DEXAMETHASONE / NEOMYCINE]").withFormats(
            new Format("DEXODEXN5D5", "5 mg / 17500 IU, eye drops, 5 ml, bot [fr:5 mg / 17500 UI, collyre, 5 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXODEXT", "DEXAMETHASONE / TOBRAMYCIN [fr:DEXAMETHASONE / TOBRAMYCINE]").withFormats(
            new Format("DEXODEXT51D", "5 mg / 15 mg, eye drops, 5 ml, bot [fr:5 mg / 15 mg, collyre, 5 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXOFLUO", "FLUORESCEIN [fr:FLUORESCEINE]").withFormats(
            new Format("DEXOFLUO1D4", "0.5%, eye drops, ster, 0.4 ml, unidose, amp. [fr:0.5%, collyre, stérile, 0.4 ml, unidose, amp.]", Unit.ML),
            new Format("DEXOFLUO2D5", "2%, eye drops, sterile, 0.5 ml, unidose, amp [fr:2%, collyre, stérile, 0.5 ml, unidose, amp.]", Unit.ML)
        ),
        new Drug("DEXOGANC", "GANCICLOVIR [fr:GANCICLOVIR]").withFormats(
            new Format("DEXOGANC1G", "0.15%, eye gel, sterile [fr:0.15%, gel ophtalmique, stérile]", Unit.ML)
        ),
        new Drug("DEXOOXYB", "OXYBUPROCAINE [fr:OXYBUPROCAINE]").withFormats(
            new Format("DEXOOXYB1", "0.4%, eye drops, ster, 0.5 ml, unidose, amp. [fr:0.4%, collyre, stér, 0.5 ml, unidose, amp.]", Unit.ML)
        ),
        new Drug("DEXOPHEE", "PHENYLEPHRINE hydrochloride [fr:PHENYLEPHRINE chlorhydrate]").withFormats(
            new Format("DEXOPHEE5D", "5%, eye drops [fr:5%, collyre]", Unit.ML)
        ),
        new Drug("DEXOPILO", "PILOCARPINE hydrochloride [fr:PILOCARPINE chlorhydrate]").withFormats(
            new Format("DEXOPILO2D1", "2%, eye drops, sterile, 10 ml, bot [fr:2%, collyre, stérile, 10 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXORIFM", "RIFAMYCINE [fr:RIFAMYCINE]").withFormats(
            new Format("DEXORIFM1D1", "sodium, 1000.000lU / 100 ml, eye drops, 10 ml, bot. [fr:sodique, 1000000Ul / 100 ml, collyre, 10 ml, fl.]", Unit.ML),
            new Format("DEXORIFM1O5", "1000.000 UI / 100 g, eye ointment, sterile, 5 g, tube [fr:1000000 UI / 100 g, pom. ophtalm, stérile, 5 g, tube]", Unit.MG)
        ),
        new Drug("DEXOSODC", "SODIUM CHLORIDE [fr:SERUM PHYSIOLOGIQUE]").withFormats(
            new Format("DEXOSODC9D5", "0.9%, eye drops, sterile, 5 ml [fr:chlorure de sodium 0.9%, stérile, 5 ml]", Unit.ML)
        ),
        new Drug("DEXOTETR", "TETRACYCLINE hydrochloride [fr:TETRACYCLINE chlorhydrate]").withFormats(
            new Format("DEXOTETR1O5", "1%, eye ointment, ster, 5 g, tube [fr:1%, pommade opht., stér, 5 g, tube]", null)
        ),
        new Drug("DEXOTROP", "TROPICAMIDE [fr:TROPICAMIDE]").withFormats(
            new Format("DEXOTROP1D0", "1% eye drops 0.5 ml, unidose, amp [fr:1% collyre 0.5 ml, unidose, amp.]", Unit.ML),
            new Format("DEXOTROP5D4", "0.5%, eye drops, sterile, 0.4 ml, unidose, amp. [fr:0.5%, collyre, stérile, 0.4 ml, unidose, amp.]", Unit.ML)
        ),
        new Drug("DEXTACIV", "ACICLOVIR [fr:ACICLOVIR]").withFormats(
            new Format("DEXTACIV5C1", "5%, cream, 10 g, tube [fr:5%, crème, 10 g, tube]", Unit.ML)
        ),
        new Drug("DEXTALCD", "DENATURED ALCOHOL [fr:ALCOOL DENATURE]").withFormats(
            new Format("DEXTALCDB5", "500 ml, bot. [fr:500 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTALCO", "ALCOHOL-BASED HAND RUB [fr:HYDRO-ALCOOLIQUE]").withFormats(
            new Format("DEXTALCO1G", "gel, 100 ml, bot. [fr:gel, 100 ml, fl.]", Unit.ML),
            new Format("DEXTALCO3G", "gel, 30 ml, bot. [fr:gel, 30 ml, fl.]", Unit.ML),
            new Format("DEXTALCO5S", "solution, 500 ml, bot. [fr:solution, 500 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTANTH", "ANTIHAEMORROID [fr:ANTI HEMORROIDAIRE]").withFormats(
            new Format("DEXTANTH1C2", "cream, 25 g, tube [fr:crème, 25 g, tube]", Unit.MG),
            new Format("DEXTANTH1O2", "ointment, 25 g, tube [fr:pommade, 25 g, tube]", Unit.MG)
        ),
        new Drug("DEXTARTS", "ARTESUNATE [fr:ARTESUNATE]").withFormats(
            new Format("DEXTARTS1RC", "100 mg, rectal caps. [fr:100 mg, caps. rectale]", Unit.CAPSULE)
        ),
        new Drug("DEXTBENS", "BENZOIC ACID / SALICYLIC ACID [fr:ACIDE BENZOIQUE / ACIDE SALICYLIQUE]").withFormats(
            new Format("DEXTBENS6O4", "6% / 3%, ointment, 40 g, tube [fr:6% / 3%, pom., 40 g, tube]", Unit.ML)
        ),
        new Drug("DEXTBENZ", "BENZYL BENZOATE [fr:BENZOATE DE BENZYLE]").withFormats(
            new Format("DEXTBENZ2L1", "25%, lotion, 1 l, bot. [fr:25%, lotion, 1 l, fl.]", Unit.ML)
        ),
        new Drug("DEXTBETM", "BETAMETHASONE dipropionate [fr:BETAMETHASONE dipropionate]").withFormats(
            new Format("DEXTBETM5C3", "eq. 0.05% base, cream, 30 g, tube [fr:éq. 0.05% base, crème, 30 g, tube]", Unit.ML)
        ),
        new Drug("DEXTCALA", "CALAMINE [fr:CALAMINE]").withFormats(
            new Format("DEXTCALA1L5", "15%, lotion, 500 ml, bot. [fr:15%, lotion, 500 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTCHLH", "CHLORHEXIDINE [fr:CHLORHEXIDINE]").withFormats(
            new Format("DEXTCHLH2AS", "digluconate 2%, aqueous solution, 100 ml, bot. [fr:gluconate 2%, solution aqueuse, 100 ml fl.]", Unit.ML),
            new Format("DEXTCHLH2S", "digluconate 0.2%, mouthwash, sol., 300 ml, bot. [fr:digluconate 0.2%, bain de bouche, sol., 300 ml, fl]", Unit.ML),
            new Format("DEXTCHLH2S5", "0.2%, aqueous solution, 5 ml, unidose [fr:0.2%, solution aqueuse, 5 ml, unidose]", Unit.ML),
            new Format("DEXTCHLH2SA2", "2%, alcohol solution, 250 ml, bot. [fr:2%, solution alcoolique, 250 ml, fl.]", Unit.ML),
            new Format("DEXTCHLH5S1", "digluconate 5%, solution, 1 l, bot. [fr:digluconate 5%, solution, 1 l, fl.]", Unit.ML),
            new Format("DEXTCHLH5S9", "digluc. 0.5 ml / 0.5 g / 100 ml, mouthwash, sol, 90 ml [fr:digluc. 0.5 ml / 0.5 g / 100 ml, bain d. bouche, sol, 90 ml]", Unit.ML),
            new Format("DEXTCHLH7G2", "digluconate 7.1%, gel, 20 g tube [fr:digluconate 7.1%, gel, 20 g tube]", Unit.ML),
            new Format("DEXTCHLH7G3", "digluconate 7.1%, gel, 3 g sachet [fr:digluconate 7.1%, gel, 3 g sachet]", Unit.ML),
            new Format("DEXTCHLHA2S2", "2%, 70% isopropyl alcohol, sol., 250 ml, bot. [fr:2%, 70% d'alcool isopropylique, sol., 250 ml, fl.]", Unit.ML),
            new Format("DEXTCHLHA2W", "2%, 70% isopropyl alcohol, WIPE [fr:2%, 70% d'alcool isopropylique, LINGETTE]", Unit.ML),
            new Format("DEXTCHLHSP4", "digluconate 4%, soap, 500 ml, bot. [fr:digluconate 4%, savon, 500 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTCIPR", "CIPROFLOXACIN [fr:CIPROFLOXACINE]").withFormats(
            new Format("DEXTCIPR1D", "0.3%, ear / eye drops, sterile, bot. [fr:0.3%, gttes auriculaires / collyre, stérile, fl]", Unit.ML)
        ),
        new Drug("DEXTCLOT", "CLOTRIMAZOLE [fr:CLOTRIMAZOLE]").withFormats(
            new Format("DEXTCLOT1C2", "1%, cream, 20 g, tube [fr:1%, crème, 20 g, tube]", Unit.ML),
            new Format("DEXTCLOT5T", "500 mg, vaginal tab. + applicator [fr:500 mg, comp. vaginal + applicateur]", Unit.TABLET)
        ),
        new Drug("DEXTCOLD", "COLD CREAM [fr:COLD CREAM]").withFormats(
            new Format("DEXTCOLD1C", "cream, 1000 ml, jar [fr:crème, 1000 ml, pot]", Unit.ML)
        ),
        new Drug("DEXTDEET", "D. E. E. T. [fr:D. E. E. T.]").withFormats(
            new Format("DEXTDEET1C", "anti-mosquito repellent lotion, 30% [fr:lotion répulsive anti-moustique, 30%]", Unit.ML)
        ),
        new Drug("DEXTDIAZ", "DIAZEPAM [fr:DIAZEPAM]").withFormats(
            new Format("DEXTDIAZ1RS", "4 mg / ml, rectal sol., 2.5 ml, tube [fr:4 mg / ml, sol. rectale, 2.5 ml, tube]", Unit.ML),
            new Format("DEXTDIAZ2RS", "2 mg / 1 ml, rectal sol., 1.25 ml, tube [fr:2 mg / 1 ml, sol. rectale, 1.25 ml, tube]", Unit.ML)
        ),
        new Drug("DEXTDICL", "DICLOFENAC [fr:DICLOFENAC]").withFormats(
            new Format("DEXTDICL1G5", "1%, gel, 50 g, tube [fr:1%, gel, 50 g, tube]", Unit.ML)
        ),
        new Drug("DEXTDINO", "DINOPROSTONE [fr:DINOPROSTONE]").withFormats(
            new Format("DEXTDINO1G", "1 mg, vaginal gel, sterile [fr:1 mg, gel vaginal stérile]", Unit.MG)
        ),
        new Drug("DEXTENEM", "ENEMA [fr:LAVEMENT]").withFormats(
            new Format("DEXTENEM5RS", "rectal sol., 5 ml, tube [fr:sol. rectale, 5 ml, tube]", Unit.ML)
        ),
        new Drug("DEXTFENT", "FENTANYL [fr:FENTANYL]").withFormats(
            new Format("DEXTFENT2TP", "2.1 mg / 5.25cm2, 12 μg / h, transdermal patch [fr:2.1 mg / 5.25cm2, 12 μg / h, dispositif transdermique]", Unit.MG),
            new Format("DEXTFENT4TP", "4.2 mg, 25 μg / h, transdermal patch [fr:4.2 mg, 25 μg / h, dispositif transdermique]", Unit.MG)
        ),
        new Drug("DEXTFUSI", "FUSIDIC ACID [fr:ACIDE FUSIDIQUE]").withFormats(
            new Format("DEXTFUSI2C3", "2%, cream, 30 g, tube [fr:2%, crème, 30 g, tube]", Unit.ML)
        ),
        new Drug("DEXTGLYP", "GLYCEROL / PARAFFIN [fr:GLYCEROL / PARAFFINE]").withFormats(
            new Format("DEXTGLYP2C", "15% / 10%, cream, 250 g, tube [fr:15% / 10%, crème, 250 g, tube]", Unit.ML)
        ),
        new Drug("DEXTHYDR", "HYDROCORTISONE (acetate or base) [fr:HYDROCORTISONE (acétate ou base)]").withFormats(
            new Format("DEXTHYDR1C1", "1%, cream, 15 g, tube [fr:1%, crème, 15 g, tube]", Unit.ML),
            new Format("DEXTHYDR1O1", "1%, ointment, 15 g, tube [fr:1%, pommade, 15 g, tube]", Unit.ML)
        ),
        new Drug("DEXTHYPE", "HYDROGEN PEROXIDE [fr:PEROXYDE D'HYDROGÈNE]").withFormats(
            new Format("DEXTHYPE3B2", "3%, sol., 250 ml, bot. [fr:3%, sol., 250 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTHYSU", "HYALURONATE sodium / SILVER SULFADIAZINE [fr:HYALURONATE de sodium / SULFADIAZINE argent]").withFormats(
            new Format("DEXTHYSU1C", "/ cream, 100 g, tube [fr:/ ., crème, 100 g, tube]", Unit.MG)
        ),
        new Drug("DEXTIODP", "POLYVIDONE IODINE [fr:POLYVIDONE IODEE]").withFormats(
            new Format("DEXTIODP1G3", "10%, gel, tube of 30 g [fr:10%, gel, tube de 30 g]", Unit.ML),
            new Format("DEXTIODP1S2", "10%, solution, 200 ml, dropper bot. [fr:10%, solution, 200 ml, fl. verseur]", Unit.ML),
            new Format("DEXTIODPS4", "surgical scrub, 4%, 125 ml, bot. [fr:savon germicide, 4%, 125 ml, fl.]", Unit.ML),
            new Format("DEXTIODPS75", "surgical scrub, 7.5%, 500 ml, bot. [fr:savon germicide, 7.5%, 500 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTIUDE", "INTRA UTERINE DEVICE [fr:DISPOSITIF INTRA UTERIN]").withFormats(
            new Format("DEXTIUDE1L", "LEVONORGESTREL, 52 mg (LNG-IUD 52) [fr:LEVONORGESTREL, 52 mg (LNG-DIU 52)]", Unit.MG)
        ),
        new Drug("DEXTLICH", "LIDOCAINE / CHLORHEX. [fr:LIDOCAINE / CHLORHEX.]").withFormats(
            new Format("DEXTLICH2J", "2% / . digluc. 0.25%, jelly, 11 ml, ster, syr. [fr:2% / . digluc. 0.25%, gel, 11 ml, stér, ser.]", Unit.ML)
        ),
        new Drug("DEXTLIDO", "LIDOCAINE [fr:LIDOCAINE]").withFormats(
            new Format("DEXTLIDO2J3", "2%, jelly, sterile, tube [fr:2%, gel, stérile, tube]", Unit.ML)
        ),
        new Drug("DEXTLIDP", "LIDOCAINE / PRILOCAINE [fr:LIDOCAINE / PRILOCAINE]").withFormats(
            new Format("DEXTLIDP2C5", "2.5% / 2.5%, cream, 5 g, tube [fr:2.5% / 2.5%, crème, 5 g, tube]", Unit.ML)
        ),
        new Drug("DEXTMAFE", "MAFENIDE acetate [fr:MAFENIDE acetate]").withFormats(
            new Format("DEXTMAFE4C", "cream, 453.6 g, jar [fr:crème, 453.6 g, pot]", Unit.MG)
        ),
        new Drug("DEXTMALA", "MALATHION [fr:MALATHION]").withFormats(
            new Format("DEXTMALA5L", "500 mg / 100 ml, lotion, bot. [fr:500 mg / 100 ml, lotion, fl.]", Unit.ML)
        ),
        new Drug("DEXTMICO", "MICONAZOLE nitrate [fr:MICONAZOLE nitrate]").withFormats(
            new Format("DEXTMICO2C3", "2%, cream, 30 g, tube [fr:2%, crème, 30 g, tube]", Unit.ML)
        ),
        new Drug("DEXTMOSQ", "ANTIPRURITIC CREAM [fr:CREME ANTIPRURIGINEUSE]").withFormats(
            new Format("DEXTMOSQ1C", "after mosquito bites, tube [fr:après piqûres de moustiques, tube]", Unit.MG)
        ),
        new Drug("DEXTMUPI", "MUPIROCIN [fr:MUPIROCINE]").withFormats(
            new Format("DEXTMUPI2O1", "2%, ointment, 15 g, tube [fr:2%, pommade, 15 g, tube]", Unit.ML)
        ),
        new Drug("DEXTOFLO", "OFLOXACIN [fr:OFLOXACINE]").withFormats(
            new Format("DEXTOFLO1S5", "3 mg / ml, ear sol., 0.5 ml, monodose [fr:3 mg / ml, sol. auriculaire, 0.5 ml, unidose]", Unit.ML)
        ),
        new Drug("DEXTPARA", "PARACETAMOL (acetaminophen) [fr:PARACETAMOL (acétaminophène)]").withFormats(
            new Format("DEXTPARA12SU", "120 mg, suppository [fr:120 mg, suppositoire]", Unit.MG),
            new Format("DEXTPARA12SU1", "125 mg, suppository [fr:125 mg, suppositoire]", Unit.MG),
            new Format("DEXTPARA2SU", "240 mg, suppository [fr:240 mg, suppositoire]", Unit.MG),
            new Format("DEXTPARA5SU", "500 mg, suppository [fr:500 mg, suppositoire]", Unit.MG)
        ),
        new Drug("DEXTPERM", "PERMETHRIN [fr:PERMETHRINE]").withFormats(
            new Format("DEXTPERM1L1", "1%, lotion, bot. [fr:1%, lotion, fl.]", Unit.ML),
            new Format("DEXTPERM5T", "5%, cream, tube [fr:5% crème, tube]", Unit.ML)
        ),
        new Drug("DEXTPHEL", "PHENASONE / LIDOCAINE HCl [fr:PHENASONE / LIDOCAINE HCl]").withFormats(
            new Format("DEXTPHEL41D1", "4% / 1%, ear drops, 15 ml, bot. [fr:4% / 1%, gttes auric, 15 ml, fl.]", Unit.ML)
        ),
        new Drug("DEXTPODO", "PODOPHYLLOTOXIN [fr:PODOPHYLLOTOXINE]").withFormats(
            new Format("DEXTPODO5S3", "0.5%, solution, 3.5 ml, + 30 applicator tips [fr:0.5%, solution, 3.5 ml, + 30 applicateurs]", Unit.ML)
        ),
        new Drug("DEXTSILN", "SILVER NITRATE [fr:NITRATE D'ARGENT]").withFormats(
            new Format("DEXTSILN1U", "40%, pencil [fr:40%, crayon]", Unit.ML)
        ),
        new Drug("DEXTSUCE", "SILVER SULFADIAZINE / CERIUM nitrate [fr:SULFADIAZINE ARGENTIQUE / CERIUM nitrate]").withFormats(
            new Format("DEXTSUCE51C", "5 g / 11 g, cream, 500 g, pot [fr:5 g / 11 g, crème, 500 g]", Unit.MG)
        ),
        new Drug("DEXTSULZ", "SULFADIAZINE SILVER [fr:SULFADIAZINE ARGENTIQUE]").withFormats(
            new Format("DEXTSULZ1C5", "1%, cream, sterile, 50 g, tube [fr:1%, crème, stérile, 50 g, tube]", Unit.ML),
            new Format("DEXTSULZ1CJ", "1%, cream, sterile, 500 g, jar [fr:1%, crème, stérile, 500 g, pot]", Unit.ML)
        ),
        new Drug("DEXTYINO", "ZINC OXIDE [fr:OXYDE DE ZINC]").withFormats(
            new Format("DEXTYINO15O1", "15%, ointment, 100 g, jar [fr:15%, pommade, 100 g, pot]", Unit.ML),
            new Format("DEXTYINO1O1", "10%, ointment, 100 g, tube [fr:10%, pommade, 100 g, tube]", Unit.ML)
        )
    );

    Category VACCINE = new Category("DVAC", "vaccines/immunoglobulins", false).withDrugs(
        new Drug("DVACDTUB", "TUBERCULIN [fr:TUBERCULINE]").withFormats(
            new Format("DVACDTUB5T", "5 TU / 0.1 ml, multidose, 1 dose, vial. [fr:5 UI / 0.1 ml, multidose, 1 dose, fl.]", Unit.ML)
        ),
        new Drug("DVACIMAS", "IMMUNOGLOBULIN AFRICAN SNAKE ANTIVENOM [fr:IMMUNOGLOBULINE ANTIVENIN SERPENTS AFRICAINS]").withFormats(
            new Format("DVACIMAS2V", "EchiTab-Plus, vial [fr:EchiTab-Plus, fl]", Unit.MG),
            new Format("DVACIMAS3A", "SAIMR, 10 ml amp. [fr:SAIMR, 10 ml amp]", Unit.ML)
        ),
        new Drug("DVACIMHB", "IMMUNOGLOBULIN HUMAN HEPATITIS B [fr:IMMUNOGLOBULINE HUMAINE HEPATITE B]").withFormats(
            new Format("DVACIMHB1V", "180 IU / ml, 1 ml, vial [fr:180 UI / ml, 1 ml, fl.]", Unit.ML)
        ),
        new Drug("DVACIMHD", "IMMUNOGLOBULIN HUMAN anti-D [fr:IMMUNOGLOBULINE HUMAINE anti-D]").withFormats(
            new Format("DVACIMHD1S", "300 µg, syringe [fr:300 µg, seringue]", Unit.MG),
            new Format("DVACIMHD1V", "300 µg, powder + diluent, vial [fr:300 µg, poudre + solvant, fl.]", Unit.MG)
        ),
        new Drug("DVACIMHR", "IMMUNOGLOBULIN HUMAN ANTIRABIES [fr:IMMUNOGLOBULINE HUM.]").withFormats(
            new Format("DVACIMHR3V", "150 UI / ml, 2 ml, vial [fr:ANTIRABIQUES, 150 UI / ml, 2 ml, fl.]", Unit.ML),
            new Format("DVACIMHR3V1", "300 IU / ml, 1 ml, vial [fr:ANTIRABIQUE, 300 UI / ml, 1 ml, fl.]", Unit.ML),
            new Format("DVACIMHR3V5", "300 IU / ml, 5 ml, vial [fr:ANTIRABIQUE, 300 UI / ml, 5 ml, fl.]", Unit.ML)
        ),
        new Drug("DVACIMPH", "IMMUNOGLOBULIN [fr:IMMUNOGLOBULINE]").withFormats(
            new Format("DVACIMPH1V", "polyvalent, human, 0.1 g / ml, 100 ml, vial [fr:polyvalent, humaine, 0.1 g / ml, 100 ml, fl.]", Unit.ML)
        ),
        new Drug("DVACIMTE", "IMMUNOGLOBULIN HUMAN ANTITETANUS [fr:IMMUNOGLOBULINE HUM. ANTITETANIQUE]").withFormats(
            new Format("DVACIMTE2S", "250 IU / ml, syr. [fr:250 UI / ml, sering.]", Unit.MG)
        ),
        new Drug("DVACVBCG", "VACCINE BCG [fr:VACCIN BCG]").withFormats(
            new Format("DVACVBCG3SD", "DILUENT, 1 dose, multidose vial [fr:SOLVANT, 1 dose, multidose fl.]", Unit.MG),
            new Format("DVACVBCG3VD", "1 dose, multidose vial, 0.05 ml / dose [fr:1 dose, fl. multidose, 0.05 ml / dose]", Unit.ML)
        ),
        new Drug("DVACVCHO", "VACCINE CHOLERA, ORAL, monodose [fr:VACCIN CHOLERA, ORAL, monodose]").withFormats(
            new Format("DVACVCHO1PT", "se, 1.5 ml, plastic tube [fr:se, 1.5 ml, tube plast.]", Unit.ML),
            new Format("DVACVCHO1V", "se, 1.5 ml, vial [fr:se, 1.5 ml, fl]", Unit.ML)
        ),
        new Drug("DVACVDHH", "VACCINE DPT / HEPATITIS B / Hib [fr:VACCIN DTC / HEPATITE B / Hib]").withFormats(
            new Format("DVACVDHH1VD", "1 dose, multidose vial [fr:1 dose, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVDTB", "VACCINE Td (tetanus / diphtheria booster) [fr:VACCIN Td (tétanos / diphtérie dose rappel)]").withFormats(
            new Format("DVACVDTB1VD", "1 dose, multidose vial [fr:1 dose, fl. multid.]", Unit.MG)
        ),
        new Drug("DVACVENC", "VACCINE JAPANESE ENCEPHALITIS [fr:VACCIN ENCEPHALITE JAPONAISE]").withFormats(
            new Format("DVACVENC1S", "monodose, syringe, 0.5 ml [fr:monodose, seringue, 0.5 ml]", Unit.ML)
        ),
        new Drug("DVACVHEA", "VACCINE HEPATITIS A [fr:VACCIN HEPATITE A]").withFormats(
            new Format("DVACVHEA1S", "1 dose, adult, monodose, syringe [fr:1 dose, adult, monodose, seringue]", Unit.MG)
        ),
        new Drug("DVACVHEB", "VACCINE HEPATITIS B [fr:VACCIN HEPATITE B]").withFormats(
            new Format("DVACVHEB1U", "1 adult dose, monodose, uniject [fr:1 dose adulte, monodose, uniject]", Unit.MG),
            new Format("DVACVHEB1VD", "1 adult dose, multidose vial [fr:1 dose adulte, fl. multidose]", Unit.MG),
            new Format("DVACVHEB2V", "1 adult dose, monodose, 1 ml, vial [fr:1 dose adulte, monodose, 1 ml, fl.]", Unit.ML),
            new Format("DVACVHEB3V", "1 child dose, monodose, 0.5 ml, vial [fr:1 dose enfant, monodose, 0.5 ml, fl.]", Unit.ML),
            new Format("DVACVHEB3VD", "1 child dose, multidose vial [fr:1 dose enfant, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVHIB", "VACCINE HAEMOPHILUS INFLUENZAE type b [fr:VACCIN HAEMOPHILUS INFLUENZAE type b]").withFormats(
            new Format("DVACVHIB1S", "monodose, 0.5 ml, syr. [fr:monodose, 0.5 ml, ser.]", Unit.ML)
        ),
        new Drug("DVACVHPV", "VACCINE HPV [fr:VACCIN HPV]").withFormats(
            new Format("DVACVHPV2V", "bivalent, monodose, 0.5 ml, vial [fr:bivalent, monodose, 0.5 ml, fl.]", Unit.ML),
            new Format("DVACVHPV4V", "quadrivalent, monodose, 0.5 ml, vial [fr:quadrivalent, monodose, 0.5 ml, fl.]", Unit.ML)
        ),
        new Drug("DVACVMEA", "VACCINE MEASLES [fr:VACCIN ROUGEOLE]").withFormats(
            new Format("DVACVMEA2SD", "DILUENT, 1 dose, multidose vial [fr:SOLVANT, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVMEA2VD", "1 dose, multidose vial [fr:1 dose, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVMEN", "VACCINE MENINGITIS [fr:VACCIN MENINGITE]").withFormats(
            new Format("DVACVMEN1VWCJ", "MENINGITIS CJ A+C+W135+Y, monod. + dil. 0.5 ml (Menveo) [fr:MENINGITE CJ A+C+W135+Y, monod. + solv. 0.5 ml (Menveo)]", Unit.ML),
            new Format("DVACVMEN2VWCJ", "MENINGITIS CJ A+C+W135+Y, monod., vial (Menactra) [fr:MENINGITE CJ A+C+W135+Y, monod. fl. (Menactra)]", Unit.MG),
            new Format("DVACVMEN3VWCJ", "MENINGITIS CJ A+C+W135+Y, monod. +dil. 0.5 ml (Nimenrix) [fr:MENINGITE CJ A+C+W135+Y, monod. +solv. 0.5 ml (Nimenrix)]", Unit.ML),
            new Format("DVACVMENA1SD", "MENINGOCOCCAL A CONJUGATE, 1-29years) DILUENT 1 dose, multidose v. [fr:MENINGOCOQUE A CONJUGUE, 1-29 ans) SOLVANT 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVMENA1VD", "MENINGOCOCCAL A CONJUGATE, 1-29years, 1dose, multid. v [fr:MENINGOCOQUE A CONJUGUE, 1-29 ans, 1dose, fl. multid.]", Unit.MG),
            new Format("DVACVMENA2SD", "MENINGOCOCCAL A CONJUGATE, 3-24months) DILUENT, 1 dose, multidose [fr:MENINGOCOQUE A CONJUGUE, 3-24 mois) SOLVANT, 1 dose, multidose]", Unit.MG),
            new Format("DVACVMENA2VD", "MENINGOCOCCAL A CONJ. 3-24months, 1dose, multid. vial [fr:MENINGOCOQUE A CONJ. 3-24 mois, 1dose, fl. multid.]", Unit.MG)
        ),
        new Drug("DVACVMER", "MEASLES / RUBELLA VACCINE [fr:VACCIN ROUGEOLE /]").withFormats(
            new Format("DVACVMER1SD", "Diluent multidose, 1 dose, bottle [fr:ROUBEOLE, ) Solvant multidose, 1 dose, fl.]", Unit.MG),
            new Format("DVACVMER1VD", "multidose, 1 dose, vial [fr:RUBEOLE, multidose, 1 dose, fl.]", Unit.MG)
        ),
        new Drug("DVACVMMR", "VACCINE MMR (measles / mumps / rubella) [fr:VACCIN ROR (rougeole / oreillons / rubéole)]").withFormats(
            new Format("DVACVMMR1SD", "DILUENT, 1 dose, multidose vial [fr:SOLVANT, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVMMR1VD", "1 dose, multidose vial [fr:1dose, fl. multidose]", Unit.MG),
            new Format("DVACVMMR2S", "DILUENT, monodose, amp. [fr:SOLVANT, monod amp.]", Unit.MG),
            new Format("DVACVMMR2V", "monodose vial [fr:monodose, fl.]", Unit.MG)
        ),
        new Drug("DVACVPCV", "VACCINE PNEUMOCOCCAL CONJUGATE [fr:VACCIN PNEUMOCOQUES CONJUGUE]").withFormats(
            new Format("DVACVPCV13VD", "PCV13, 1 dose, vial, multidose [fr:PCV13, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVPCV13VDN", "PCV13.1dose, multid. vl noGAVI [fr:PCV13, 1dose, fl. multid. nonGAVI]", Unit.MG),
            new Format("DVACVPCV13VDS", "PCV13, 1dose, multid. vial spec [fr:PCV13, 1dose, fl. multid. spéc.]", Unit.MG),
            new Format("DVACVPCV2VD", "PCV10, 1 dose, vial, multidose [fr:PCV10, 1 dose, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVPOI", "VACCINE POLIOMYELITIS, INACTIVATED [fr:VACCIN POLIO]").withFormats(
            new Format("DVACVPOI1VD", "D, 1 dose, multidose vial [fr:INACTIVE, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVPOI2S", "D, 0.5 ml, monodose syringe [fr:INACTIVE (IPV) inject, monodose, 0.5 ml, sering]", Unit.ML)
        ),
        new Drug("DVACVPOL", "VACCINE POLIOMYELITIS, BIVALENT ORAL [fr:VACCIN POLIO, BIVALENT ORAL]").withFormats(
            new Format("DVACVPOL13BD", "L, 1 dose, multidose vial [fr:L, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVPOL13DR", "L, DROPPER [fr:L, COMPTE-GOUTTE]", Unit.MG)
        ),
        new Drug("DVACVPPV", "VACCINE PNEUMOCOCCAL polysaccharide [fr:VACCIN PNEUMOCOQUE polysaccharide]").withFormats(
            new Format("DVACVPPV23S", "23, monodose, 0.5 ml, syr. [fr:23, monodose, 0.5 ml, ser.]", Unit.ML)
        ),
        new Drug("DVACVRAB", "VACCINE RABIES, CCV, cell culture, monodose [fr:VACCIN ANTIRABIQUE, VCC, culture cellulaire, monodose]").withFormats(
            new Format("DVACVRAB1V", "ose, vial [fr:ose, fl.]", Unit.MG),
            new Format("DVACVRAB2S", "ose, syringe [fr:ose, ser.]", Unit.MG),
            new Format("DVACVRAB3V", "ose, vial [fr:ose, fl.]", Unit.MG)
        ),
        new Drug("DVACVROT", "VACCINE ROTAVIRUS [fr:VACCIN ROTAVIRUS]").withFormats(
            new Format("DVACVROT1T", "ORAL (Rotarix), monodose, 1.5 ml, tube [fr:ORAL (Rotarix), monodose, 1.5 ml, tube]", Unit.ML)
        ),
        new Drug("DVACVTET", "VACCINE TT (tetanus) [fr:VACCIN TT (tétanos)]").withFormats(
            new Format("DVACVTET1S", "monodose, 0.5 ml, syringe [fr:monodose, 0.5 ml, seringue]", Unit.ML),
            new Format("DVACVTET1VD", "1 dose, multidose vial [fr:1 dose, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVTYP", "VACCINE TYPHOID [fr:VACCIN TYPHOIDIQUE]").withFormats(
            new Format("DVACVTYP1S", "polysaccharide 25 µg, monodosis, 0.5 ml, syringe [fr:polyosidique 25 µg, monodose, 0.5 ml, seringue]", Unit.ML),
            new Format("DVACVTYP2VD", "Typhim Vi, 1 dose, multidose vial [fr:Typhim Vi, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVTYPC1VD", "CONJUGATE, 1dose, multidose vial [fr:CONJUGUE, 1 dose, fl. multidose]", Unit.MG)
        ),
        new Drug("DVACVYEF", "VACCINE YELLOW FEVER [fr:VACCIN FIEVRE JAUNE]").withFormats(
            new Format("DVACVYEF1S", "monodose amp. + syr. solvent 0.5 ml [fr:monodose amp. + seringue solvant 0.5 ml]", Unit.ML),
            new Format("DVACVYEF2SD", "DILUENT, 1 dose, multidose vial [fr:SOLVANT, 1 dose, fl. multidose]", Unit.MG),
            new Format("DVACVYEF2VD", "1 dose, multidose vial [fr:1 dose, fl. multidose]", Unit.MG)
        )
    );

    // ==== END GENERATED OUTPUT ====

    CatalogIndex INDEX = new CatalogIndex(ORAL, INJECTABLE, PERFUSION, EXTERNAL, VACCINE)
        .withRoutes(UNSPECIFIED, PO, IV, SC, IM, IO, OC)
        .withDosageUnits(
            Unit.TABLET, Unit.CAPSULE,
            Unit.G, Unit.MG, Unit.MCG,
            Unit.L, Unit.ML,
            Unit.IU,
            Unit.DROP, Unit.PUFF,
            Unit.AMPOULE, Unit.SACHET,
            Unit.OVULE, Unit.SUPP);
}
