package com.crescendo.apps.jobsearch;

import java.net.URI;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Registry of 150+ selective Tier-1 tech companies, Indian product unicorns,
 * quantitative trading/HFT firms, AI research labs, semiconductor leaders,
 * and global GCC engineering organizations in India.
 *
 * Provides high-speed fuzzy matching to filter out mass IT recruiters and third-party agencies.
 */
public final class TargetCompanyRegistry {

    private TargetCompanyRegistry() {}

    public record CuratedCompany(
            String canonicalName,
            List<String> categories,
            List<String> aliases,
            List<String> urlKeywords
    ) {}

    public static final String CAT_TIER1 = "Tier-1 Tech & Cloud";
    public static final String CAT_BANKS_HUBS = "Global Tech Hubs & Banks";
    public static final String CAT_FINTECH_UNICORNS = "Fintech & Indian Unicorns";
    public static final String CAT_QUANT_HFT = "Quant & HFT";
    public static final String CAT_FRONTIER_AI = "Frontier AI & Research";
    public static final String CAT_SAAS_DEVTOOLS = "Cloud, SaaS & DevTools";
    public static final String CAT_SEMICONDUCTOR = "Semiconductor & Systems";
    public static final String CAT_STRATEGY = "Selective Strategy & Consulting";

    public static final List<String> ALL_CATEGORIES = List.of(
            CAT_TIER1,
            CAT_BANKS_HUBS,
            CAT_FINTECH_UNICORNS,
            CAT_QUANT_HFT,
            CAT_FRONTIER_AI,
            CAT_SAAS_DEVTOOLS,
            CAT_SEMICONDUCTOR,
            CAT_STRATEGY
    );

    private static final List<CuratedCompany> COMPANIES = new ArrayList<>();
    private static final Map<CuratedCompany, List<Pattern>> PATTERN_CACHE = new HashMap<>();

    static {
        // 1. Tier-1 Tech Giants & Cloud
        add("Google", List.of(CAT_TIER1, CAT_FRONTIER_AI), List.of("google", "alphabet", "deepmind"), List.of("google.com"));
        add("Microsoft", List.of(CAT_TIER1, CAT_FRONTIER_AI), List.of("microsoft", "msft"), List.of("microsoft.com"));
        add("Amazon / AWS", List.of(CAT_TIER1, CAT_FRONTIER_AI), List.of("amazon", "aws", "amazon web services"), List.of("amazon.jobs", "amazon.com"));
        add("Apple", List.of(CAT_TIER1, CAT_FRONTIER_AI), List.of("apple"), List.of("apple.com"));
        add("Meta", List.of(CAT_TIER1, CAT_FRONTIER_AI), List.of("meta", "facebook"), List.of("meta.com", "facebook.com"));
        add("NVIDIA", List.of(CAT_TIER1, CAT_SEMICONDUCTOR, CAT_FRONTIER_AI), List.of("nvidia"), List.of("nvidia.com"));
        add("Adobe", List.of(CAT_TIER1), List.of("adobe"), List.of("adobe.com"));
        add("Salesforce", List.of(CAT_TIER1, CAT_SAAS_DEVTOOLS), List.of("salesforce"), List.of("salesforce.com"));
        add("Oracle", List.of(CAT_TIER1), List.of("oracle"), List.of("oracle.com"));
        add("Atlassian", List.of(CAT_TIER1, CAT_SAAS_DEVTOOLS), List.of("atlassian"), List.of("atlassian.com"));
        add("Uber", List.of(CAT_TIER1), List.of("uber"), List.of("uber.com"));
        add("LinkedIn", List.of(CAT_TIER1), List.of("linkedin"), List.of("linkedin.com"));
        add("Intuit", List.of(CAT_TIER1, CAT_BANKS_HUBS), List.of("intuit"), List.of("intuit.com"));
        add("Cisco", List.of(CAT_TIER1), List.of("cisco"), List.of("cisco.com"));
        add("VMware / Broadcom", List.of(CAT_TIER1, CAT_SEMICONDUCTOR), List.of("vmware", "broadcom"), List.of("vmware.com", "broadcom.com"));
        add("ServiceNow", List.of(CAT_TIER1, CAT_SAAS_DEVTOOLS), List.of("servicenow"), List.of("servicenow.com"));
        add("SAP", List.of(CAT_TIER1), List.of("sap"), List.of("sap.com"));
        add("PayPal", List.of(CAT_TIER1, CAT_BANKS_HUBS), List.of("paypal"), List.of("paypal.com"));
        add("Stripe", List.of(CAT_TIER1, CAT_FINTECH_UNICORNS), List.of("stripe"), List.of("stripe.com"));
        add("Cloudflare", List.of(CAT_TIER1, CAT_SAAS_DEVTOOLS), List.of("cloudflare"), List.of("cloudflare.com"));
        add("Datadog", List.of(CAT_TIER1, CAT_SAAS_DEVTOOLS), List.of("datadog"), List.of("datadoghq.com"));
        add("Confluent", List.of(CAT_TIER1, CAT_SAAS_DEVTOOLS), List.of("confluent"), List.of("confluent.io"));
        add("MongoDB", List.of(CAT_TIER1, CAT_SAAS_DEVTOOLS), List.of("mongodb"), List.of("mongodb.com"));
        add("GitHub", List.of(CAT_TIER1, CAT_SAAS_DEVTOOLS), List.of("github"), List.of("github.com"));
        add("GitLab", List.of(CAT_TIER1, CAT_SAAS_DEVTOOLS), List.of("gitlab"), List.of("gitlab.com"));
        add("Red Hat", List.of(CAT_TIER1), List.of("red hat", "redhat"), List.of("redhat.com"));
        add("Booking.com", List.of(CAT_TIER1, CAT_BANKS_HUBS), List.of("booking.com", "booking"), List.of("booking.com"));
        add("Airbnb", List.of(CAT_TIER1), List.of("airbnb"), List.of("airbnb.com"));
        add("Netflix", List.of(CAT_TIER1), List.of("netflix"), List.of("netflix.com"));
        add("Palantir", List.of(CAT_TIER1), List.of("palantir"), List.of("palantir.com"));
        add("Canva", List.of(CAT_TIER1), List.of("canva"), List.of("canva.com"));

        // 2. Global Investment Banks & Engineering Centers (GCCs in India)
        add("JPMorgan Chase", List.of(CAT_BANKS_HUBS), List.of("jpmorgan", "jp morgan", "chase"), List.of("jpmorgan.com", "jpmorganchase.com"));
        add("Goldman Sachs", List.of(CAT_BANKS_HUBS), List.of("goldman sachs", "goldman"), List.of("goldmansachs.com"));
        add("Morgan Stanley", List.of(CAT_BANKS_HUBS), List.of("morgan stanley"), List.of("morganstanley.com"));
        add("Bank of America", List.of(CAT_BANKS_HUBS), List.of("bank of america", "bofa"), List.of("bankofamerica.com"));
        add("Citibank", List.of(CAT_BANKS_HUBS), List.of("citibank", "citi", "citigroup"), List.of("citigroup.com", "citi.com"));
        add("Barclays", List.of(CAT_BANKS_HUBS), List.of("barclays"), List.of("barclays.com"));
        add("HSBC", List.of(CAT_BANKS_HUBS), List.of("hsbc"), List.of("hsbc.com"));
        add("Standard Chartered", List.of(CAT_BANKS_HUBS), List.of("standard chartered", "stan chart"), List.of("sc.com"));
        add("American Express", List.of(CAT_BANKS_HUBS), List.of("american express", "amex"), List.of("americanexpress.com"));
        add("BNY", List.of(CAT_BANKS_HUBS), List.of("bny", "bny mellon", "bank of new york mellon"), List.of("bnymellon.com"));
        add("Wells Fargo", List.of(CAT_BANKS_HUBS), List.of("wells fargo"), List.of("wellsfargo.com"));
        add("Deutsche Bank", List.of(CAT_BANKS_HUBS), List.of("deutsche bank"), List.of("db.com"));
        add("UBS", List.of(CAT_BANKS_HUBS), List.of("ubs"), List.of("ubs.com"));
        add("BlackRock", List.of(CAT_BANKS_HUBS), List.of("blackrock"), List.of("blackrock.com"));
        add("Bloomberg", List.of(CAT_BANKS_HUBS), List.of("bloomberg"), List.of("bloomberg.com"));
        add("State Street", List.of(CAT_BANKS_HUBS), List.of("state street"), List.of("statestreet.com"));
        add("Capital One", List.of(CAT_BANKS_HUBS), List.of("capital one"), List.of("capitalone.com"));
        add("Visa", List.of(CAT_BANKS_HUBS), List.of("visa"), List.of("visa.com"));
        add("Mastercard", List.of(CAT_BANKS_HUBS), List.of("mastercard"), List.of("mastercard.com"));
        add("Walmart Global Tech", List.of(CAT_BANKS_HUBS), List.of("walmart global tech", "walmart"), List.of("walmart.com"));
        add("Target", List.of(CAT_BANKS_HUBS), List.of("target"), List.of("target.com"));
        add("Nike Technology", List.of(CAT_BANKS_HUBS), List.of("nike"), List.of("nike.com"));
        add("Expedia", List.of(CAT_BANKS_HUBS), List.of("expedia"), List.of("expedia.com"));
        add("Dell Technologies", List.of(CAT_BANKS_HUBS), List.of("dell technologies", "dell"), List.of("dell.com"));
        add("HP", List.of(CAT_BANKS_HUBS), List.of("hp inc", "hewlett packard", "hp"), List.of("hp.com"));
        add("Siemens", List.of(CAT_BANKS_HUBS, CAT_SEMICONDUCTOR), List.of("siemens digital industries", "siemens"), List.of("siemens.com"));
        add("Sony", List.of(CAT_BANKS_HUBS), List.of("sony research", "sony"), List.of("sony.com"));
        add("Samsung Research", List.of(CAT_BANKS_HUBS, CAT_SEMICONDUCTOR), List.of("samsung research", "samsung r&d", "samsung"), List.of("samsung.com"));

        // 3. Fintech & Premier Indian Unicorns
        add("PhonePe", List.of(CAT_FINTECH_UNICORNS), List.of("phonepe"), List.of("phonepe.com"));
        add("Razorpay", List.of(CAT_FINTECH_UNICORNS), List.of("razorpay"), List.of("razorpay.com"));
        add("CRED", List.of(CAT_FINTECH_UNICORNS), List.of("cred", "dreamplug"), List.of("cred.club"));
        add("Groww", List.of(CAT_FINTECH_UNICORNS), List.of("groww"), List.of("groww.in"));
        add("Zerodha", List.of(CAT_FINTECH_UNICORNS), List.of("zerodha"), List.of("zerodha.com"));
        add("Meesho", List.of(CAT_FINTECH_UNICORNS), List.of("meesho"), List.of("meesho.com"));
        add("Paytm", List.of(CAT_FINTECH_UNICORNS), List.of("paytm", "one97"), List.of("paytm.com"));
        add("Coinbase", List.of(CAT_FINTECH_UNICORNS), List.of("coinbase"), List.of("coinbase.com"));
        add("Pine Labs", List.of(CAT_FINTECH_UNICORNS), List.of("pine labs"), List.of("pinelabs.com"));
        add("MobiKwik", List.of(CAT_FINTECH_UNICORNS), List.of("mobikwik"), List.of("mobikwik.com"));
        add("Cashfree", List.of(CAT_FINTECH_UNICORNS), List.of("cashfree"), List.of("cashfree.com"));
        add("BharatPe", List.of(CAT_FINTECH_UNICORNS), List.of("bharatpe", "resilient innovations"), List.of("bharatpe.com"));
        add("Policybazaar", List.of(CAT_FINTECH_UNICORNS), List.of("policybazaar", "pb fintech"), List.of("policybazaar.com"));
        add("Acko", List.of(CAT_FINTECH_UNICORNS), List.of("acko"), List.of("acko.com"));
        add("Slice", List.of(CAT_FINTECH_UNICORNS), List.of("slice", "garagepreKingdom"), List.of("sliceit.com"));
        add("Fi", List.of(CAT_FINTECH_UNICORNS), List.of("fi money", "epifi"), List.of("fi.money"));
        add("Dream11", List.of(CAT_FINTECH_UNICORNS), List.of("dream11", "sporta technologies"), List.of("dream11.com"));
        add("Upstox", List.of(CAT_FINTECH_UNICORNS), List.of("upstox", "rksv"), List.of("upstox.com"));
        add("Navi", List.of(CAT_FINTECH_UNICORNS), List.of("navi technologies", "navi"), List.of("navi.com"));
        add("Jupiter", List.of(CAT_FINTECH_UNICORNS), List.of("jupiter money", "jupiter"), List.of("jupiter.money"));
        add("Flipkart", List.of(CAT_FINTECH_UNICORNS), List.of("flipkart"), List.of("flipkart.com"));
        add("Swiggy", List.of(CAT_FINTECH_UNICORNS), List.of("swiggy", "bundl technologies"), List.of("swiggy.com"));
        add("Zomato", List.of(CAT_FINTECH_UNICORNS), List.of("zomato", "eternal"), List.of("zomato.com"));
        add("Zepto", List.of(CAT_FINTECH_UNICORNS), List.of("zepto", "kiranakart"), List.of("zeptonow.com"));
        add("Myntra", List.of(CAT_FINTECH_UNICORNS), List.of("myntra"), List.of("myntra.com"));
        add("MakeMyTrip", List.of(CAT_FINTECH_UNICORNS), List.of("makemytrip", "mmt"), List.of("makemytrip.com"));
        add("Ola", List.of(CAT_FINTECH_UNICORNS), List.of("ola electric", "ola", "ani technologies"), List.of("olacabs.com"));
        add("Nykaa", List.of(CAT_FINTECH_UNICORNS), List.of("nykaa", "fsn e-commerce"), List.of("nykaa.com"));
        add("Urban Company", List.of(CAT_FINTECH_UNICORNS), List.of("urban company", "urbanclap"), List.of("urbancompany.com"));
        add("Delhivery", List.of(CAT_FINTECH_UNICORNS), List.of("delhivery"), List.of("delhivery.com"));
        add("Blinkit", List.of(CAT_FINTECH_UNICORNS), List.of("blinkit", "grofers"), List.of("blinkit.com"));
        add("Ather Energy", List.of(CAT_FINTECH_UNICORNS), List.of("ather energy", "ather"), List.of("atherenergy.com"));
        add("Rapido", List.of(CAT_FINTECH_UNICORNS), List.of("rapido", "roppen transportation"), List.of("rapido.bike"));
        add("Freshworks", List.of(CAT_FINTECH_UNICORNS, CAT_SAAS_DEVTOOLS), List.of("freshworks"), List.of("freshworks.com"));
        add("Zoho", List.of(CAT_FINTECH_UNICORNS, CAT_SAAS_DEVTOOLS), List.of("zoho"), List.of("zoho.com"));
        add("Postman", List.of(CAT_FINTECH_UNICORNS, CAT_SAAS_DEVTOOLS), List.of("postman"), List.of("postman.com"));
        add("BrowserStack", List.of(CAT_FINTECH_UNICORNS, CAT_SAAS_DEVTOOLS), List.of("browserstack"), List.of("browserstack.com"));
        add("Chargebee", List.of(CAT_FINTECH_UNICORNS, CAT_SAAS_DEVTOOLS), List.of("chargebee"), List.of("chargebee.com"));
        add("Druva", List.of(CAT_FINTECH_UNICORNS, CAT_SAAS_DEVTOOLS), List.of("druva"), List.of("druva.com"));
        add("Icertis", List.of(CAT_FINTECH_UNICORNS, CAT_SAAS_DEVTOOLS), List.of("icertis"), List.of("icertis.com"));
        add("InMobi", List.of(CAT_FINTECH_UNICORNS), List.of("inmobi"), List.of("inmobi.com"));
        add("ShareChat", List.of(CAT_FINTECH_UNICORNS), List.of("sharechat", "mohalla tech"), List.of("sharechat.com"));
        add("Moglix", List.of(CAT_FINTECH_UNICORNS), List.of("moglix"), List.of("moglix.com"));
        add("Udaan", List.of(CAT_FINTECH_UNICORNS), List.of("udaan", "hiveloop"), List.of("udaan.com"));
        add("OfBusiness", List.of(CAT_FINTECH_UNICORNS), List.of("ofbusiness", "oxyzo"), List.of("ofbusiness.com"));
        add("ElasticRun", List.of(CAT_FINTECH_UNICORNS), List.of("elasticrun"), List.of("elastic.run"));

        // 4. Quantitative Trading & High Frequency Trading (HFT)
        add("Jane Street", List.of(CAT_QUANT_HFT), List.of("jane street"), List.of("janestreet.com"));
        add("Optiver", List.of(CAT_QUANT_HFT), List.of("optiver"), List.of("optiver.com"));
        add("Tower Research", List.of(CAT_QUANT_HFT), List.of("tower research capital", "tower research"), List.of("tower-research.com"));
        add("Graviton Research Capital", List.of(CAT_QUANT_HFT), List.of("graviton research capital", "graviton"), List.of("gravitonresearch.com"));
        add("Quadeye", List.of(CAT_QUANT_HFT), List.of("quadeye"), List.of("quadeye.com"));
        add("WorldQuant", List.of(CAT_QUANT_HFT), List.of("worldquant"), List.of("worldquant.com"));
        add("IMC Trading", List.of(CAT_QUANT_HFT), List.of("imc trading", "imc"), List.of("imc.com"));
        add("Citadel", List.of(CAT_QUANT_HFT), List.of("citadel securities", "citadel"), List.of("citadel.com"));
        add("D. E. Shaw", List.of(CAT_QUANT_HFT), List.of("d. e. shaw", "d e shaw", "de shaw", "deshaw"), List.of("deshaw.com"));
        add("Two Sigma", List.of(CAT_QUANT_HFT), List.of("two sigma", "twosigma"), List.of("twosigma.com"));
        add("Millennium", List.of(CAT_QUANT_HFT), List.of("millennium management", "millennium"), List.of("mlp.com"));
        add("Arcesium", List.of(CAT_QUANT_HFT), List.of("arcesium"), List.of("arcesium.com"));
        add("AlphaGrep", List.of(CAT_QUANT_HFT), List.of("alphagrep"), List.of("alpha-grep.com"));
        add("Da Vinci", List.of(CAT_QUANT_HFT), List.of("da vinci trading", "da vinci"), List.of("davinciderivatives.com"));
        add("Quantbox", List.of(CAT_QUANT_HFT), List.of("quantbox"), List.of("quantbox.com"));

        // 5. Frontier AI Research Labs & Specialized AI
        add("OpenAI", List.of(CAT_FRONTIER_AI), List.of("openai"), List.of("openai.com"));
        add("Anthropic", List.of(CAT_FRONTIER_AI), List.of("anthropic"), List.of("anthropic.com"));
        add("Cohere", List.of(CAT_FRONTIER_AI), List.of("cohere"), List.of("cohere.com"));
        add("Mistral AI", List.of(CAT_FRONTIER_AI), List.of("mistral ai", "mistral"), List.of("mistral.ai"));
        add("Databricks", List.of(CAT_FRONTIER_AI, CAT_SAAS_DEVTOOLS), List.of("databricks"), List.of("databricks.com"));
        add("Hugging Face", List.of(CAT_FRONTIER_AI), List.of("hugging face", "huggingface"), List.of("huggingface.co"));
        add("Scale AI", List.of(CAT_FRONTIER_AI), List.of("scale ai", "scale.com"), List.of("scale.com"));
        add("AI21 Labs", List.of(CAT_FRONTIER_AI), List.of("ai21 labs", "ai21"), List.of("ai21.com"));
        add("Perplexity", List.of(CAT_FRONTIER_AI), List.of("perplexity ai", "perplexity"), List.of("perplexity.ai"));
        add("xAI", List.of(CAT_FRONTIER_AI), List.of("xai", "x.ai"), List.of("x.ai"));
        add("Sarvam AI", List.of(CAT_FRONTIER_AI), List.of("sarvam ai", "sarvam"), List.of("sarvam.ai"));
        add("Krutrim", List.of(CAT_FRONTIER_AI), List.of("krutrim"), List.of("krutrim.com"));
        add("Ema", List.of(CAT_FRONTIER_AI), List.of("ema ai", "ema"), List.of("ema.co"));
        add("Neysa", List.of(CAT_FRONTIER_AI), List.of("neysa"), List.of("neysa.ai"));
        add("Gnani.ai", List.of(CAT_FRONTIER_AI), List.of("gnani.ai", "gnani"), List.of("gnani.ai"));
        add("CoRover", List.of(CAT_FRONTIER_AI), List.of("corover"), List.of("corover.ai"));
        add("Yellow.ai", List.of(CAT_FRONTIER_AI), List.of("yellow.ai", "yellow messenger"), List.of("yellow.ai"));
        add("Uniphore", List.of(CAT_FRONTIER_AI), List.of("uniphore"), List.of("uniphore.com"));
        add("Mad Street Den", List.of(CAT_FRONTIER_AI), List.of("mad street den", "vue.ai"), List.of("madstreetden.com"));
        add("Observe.AI", List.of(CAT_FRONTIER_AI), List.of("observe.ai", "observe ai"), List.of("observe.ai"));
        add("Sigtuple", List.of(CAT_FRONTIER_AI), List.of("sigtuple"), List.of("sigtuple.com"));
        add("Qure.ai", List.of(CAT_FRONTIER_AI), List.of("qure.ai", "qure ai"), List.of("qure.ai"));
        add("Fractal", List.of(CAT_FRONTIER_AI), List.of("fractal analytics", "fractal"), List.of("fractal.ai"));
        add("Tiger Analytics", List.of(CAT_FRONTIER_AI), List.of("tiger analytics"), List.of("tigeranalytics.com"));
        add("Tredence", List.of(CAT_FRONTIER_AI), List.of("tredence"), List.of("tredence.com"));

        // 6. Cloud, SaaS & DevTools
        add("Elastic", List.of(CAT_SAAS_DEVTOOLS), List.of("elastic"), List.of("elastic.co"));
        add("Grafana Labs", List.of(CAT_SAAS_DEVTOOLS), List.of("grafana labs", "grafana"), List.of("grafana.com"));
        add("HashiCorp", List.of(CAT_SAAS_DEVTOOLS), List.of("hashicorp"), List.of("hashicorp.com"));
        add("Snowflake", List.of(CAT_SAAS_DEVTOOLS), List.of("snowflake"), List.of("snowflake.com"));
        add("Twilio", List.of(CAT_SAAS_DEVTOOLS), List.of("twilio"), List.of("twilio.com"));
        add("HubSpot", List.of(CAT_SAAS_DEVTOOLS), List.of("hubspot"), List.of("hubspot.com"));
        add("New Relic", List.of(CAT_SAAS_DEVTOOLS), List.of("new relic"), List.of("newrelic.com"));
        add("Okta", List.of(CAT_SAAS_DEVTOOLS), List.of("okta"), List.of("okta.com"));
        add("Workday", List.of(CAT_SAAS_DEVTOOLS), List.of("workday"), List.of("workday.com"));
        add("Palo Alto Networks", List.of(CAT_SAAS_DEVTOOLS), List.of("palo alto networks"), List.of("paloaltonetworks.com"));

        // 7. Semiconductor, Hardware & Systems
        add("AMD", List.of(CAT_SEMICONDUCTOR), List.of("amd", "advanced micro devices"), List.of("amd.com"));
        add("Qualcomm", List.of(CAT_SEMICONDUCTOR), List.of("qualcomm"), List.of("qualcomm.com"));
        add("Intel", List.of(CAT_SEMICONDUCTOR), List.of("intel"), List.of("intel.com"));
        add("ARM", List.of(CAT_SEMICONDUCTOR), List.of("arm"), List.of("arm.com"));
        add("Cadence", List.of(CAT_SEMICONDUCTOR), List.of("cadence design systems", "cadence"), List.of("cadence.com"));
        add("Synopsys", List.of(CAT_SEMICONDUCTOR), List.of("synopsys"), List.of("synopsys.com"));
        add("Ansys", List.of(CAT_SEMICONDUCTOR), List.of("ansys"), List.of("ansys.com"));
        add("Applied Materials", List.of(CAT_SEMICONDUCTOR), List.of("applied materials"), List.of("appliedmaterials.com"));
        add("Texas Instruments", List.of(CAT_SEMICONDUCTOR), List.of("texas instruments"), List.of("ti.com"));
        add("Analog Devices", List.of(CAT_SEMICONDUCTOR), List.of("analog devices"), List.of("analog.com"));
        add("Micron", List.of(CAT_SEMICONDUCTOR), List.of("micron technology", "micron"), List.of("micron.com"));
        add("MediaTek", List.of(CAT_SEMICONDUCTOR), List.of("mediatek"), List.of("mediatek.com"));
        add("Marvell", List.of(CAT_SEMICONDUCTOR), List.of("marvell technology", "marvell"), List.of("marvell.com"));
        add("Lam Research", List.of(CAT_SEMICONDUCTOR), List.of("lam research"), List.of("lamresearch.com"));
        add("ASML", List.of(CAT_SEMICONDUCTOR), List.of("asml"), List.of("asml.com"));
        add("Bosch", List.of(CAT_SEMICONDUCTOR), List.of("bosch global software", "robert bosch", "bosch"), List.of("bosch.com"));
        add("Continental", List.of(CAT_SEMICONDUCTOR), List.of("continental"), List.of("continental.com"));
        add("NXP", List.of(CAT_SEMICONDUCTOR), List.of("nxp semiconductors", "nxp"), List.of("nxp.com"));

        // 8. Selective Strategy & Tech Consulting
        add("McKinsey", List.of(CAT_STRATEGY), List.of("mckinsey & company", "mckinsey"), List.of("mckinsey.com"));
        add("BCG", List.of(CAT_STRATEGY), List.of("boston consulting group", "bcg"), List.of("bcg.com"));
        add("Bain", List.of(CAT_STRATEGY), List.of("bain & company", "bain"), List.of("bain.com"));
        add("ZS Associates", List.of(CAT_STRATEGY), List.of("zs associates", "zs"), List.of("zs.com"));
        add("Gartner", List.of(CAT_STRATEGY), List.of("gartner"), List.of("gartner.com"));
    }

    private static void add(String canonicalName, List<String> categories, List<String> aliases, List<String> urlKeywords) {
        CuratedCompany comp = new CuratedCompany(canonicalName, categories, aliases, urlKeywords);
        COMPANIES.add(comp);

        List<Pattern> patterns = new ArrayList<>();
        for (String alias : aliases) {
            String cleaned = cleanCompanyName(alias);
            if (!cleaned.isEmpty()) {
                patterns.add(Pattern.compile("\\b" + Pattern.quote(cleaned) + "\\b", Pattern.CASE_INSENSITIVE));
            }
        }
        PATTERN_CACHE.put(comp, patterns);
    }

    /**
     * Checks if a given job matches the curated target companies based on company name,
     * optional url, category filters, or a custom user whitelist.
     */
    public static boolean matches(
            String rawCompany,
            String jobUrl,
            Set<String> selectedCategories,
            Set<String> customWhitelist
    ) {
        String companyClean = cleanCompanyName(rawCompany);
        // Extract hostname from URL to avoid false positives from query params
        // (e.g. google_jobs_apply in UTM params matching Google, or indeed.com matching a target)
        String urlHost = extractHost(jobUrl);
        String urlPath = extractPath(jobUrl);

        // 1. If custom whitelist is provided, prioritize it
        if (customWhitelist != null && !customWhitelist.isEmpty()) {
            for (String allowed : customWhitelist) {
                String allowedClean = cleanCompanyName(allowed);
                if (allowedClean.isEmpty()) continue;
                Pattern p = Pattern.compile("\\b" + Pattern.quote(allowedClean) + "\\b", Pattern.CASE_INSENSITIVE);
                if (p.matcher(companyClean).find()) {
                    return true;
                }
                // Also check if the job URL's host matches (not query params)
                if (urlHost.contains(allowedClean)) {
                    return true;
                }
            }
            return false;
        }

        // 2. Match against curated registry
        for (CuratedCompany comp : COMPANIES) {
            // Check category filter
            if (selectedCategories != null && !selectedCategories.isEmpty()) {
                boolean catMatch = comp.categories().stream().anyMatch(selectedCategories::contains);
                if (!catMatch) continue;
            }

            // Check URL keywords against hostname + path only (NOT query params)
            // This prevents utm_campaign=google_jobs_apply from matching "Google"
            if (!urlHost.isBlank()) {
                // URL keywords are domain patterns — check hostname ends-with or exact match
                for (String uk : comp.urlKeywords()) {
                    if (urlHost.equals(uk) || urlHost.endsWith("." + uk)) return true;
                    // Also check path for ATS boards (e.g. greenhouse.io/anthropic)
                    if (!urlPath.isBlank() && urlPath.contains(uk)) return true;
                }
                // Aliases checked with word-boundary regex to avoid partial matches
                // (e.g. "rapido" alias must NOT match inside "jobrapido.com")
                String urlForMatching = urlHost + urlPath;
                for (String alias : comp.aliases()) {
                    if (alias.length() >= 5) {
                        Pattern p = Pattern.compile("\\b" + Pattern.quote(alias) + "\\b", Pattern.CASE_INSENSITIVE);
                        if (p.matcher(urlForMatching).find()) return true;
                    }
                }
            }

            // Check company name patterns
            List<Pattern> patterns = PATTERN_CACHE.get(comp);
            if (patterns != null) {
                for (Pattern p : patterns) {
                    if (p.matcher(companyClean).find()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Cleans company names by removing corporate suffixes like "Pvt Ltd", "Inc", "Corporation",
     * punctuation, and extraneous spacing for reliable matching.
     */
    public static String cleanCompanyName(String raw) {
        if (raw == null) return "";
        String s = raw.toLowerCase();
        // Remove common corporate suffixes
        s = s.replaceAll("\\b(pvt|private|ltd|limited|inc|incorporated|corp|corporation|llc|llp|gmbh|co)\\b", " ");
        s = s.replaceAll("[^a-z0-9& ]", " ");
        s = s.replaceAll("\\s+", " ");
        return s.trim();
    }

    public static List<CuratedCompany> getAllCompanies() {
        return Collections.unmodifiableList(COMPANIES);
    }

    /** Extracts the hostname from a URL, returning empty string on failure. */
    private static String extractHost(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            return new URI(url).getHost().toLowerCase();
        } catch (Exception e) {
            // Fallback: try to parse manually
            String lower = url.toLowerCase();
            int start = lower.indexOf("://");
            if (start >= 0) lower = lower.substring(start + 3);
            int end = lower.indexOf('/');
            if (end >= 0) lower = lower.substring(0, end);
            end = lower.indexOf('?');
            if (end >= 0) lower = lower.substring(0, end);
            return lower;
        }
    }

    /** Extracts the path from a URL (without query params), returning empty string on failure. */
    private static String extractPath(String url) {
        if (url == null || url.isBlank()) return "";
        try {
            String path = new URI(url).getPath();
            return path != null ? path.toLowerCase() : "";
        } catch (Exception e) {
            return "";
        }
    }
}
