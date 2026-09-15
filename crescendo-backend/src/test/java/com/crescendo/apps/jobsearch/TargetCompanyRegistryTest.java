package com.crescendo.apps.jobsearch;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TargetCompanyRegistryTest {

    @Test
    @DisplayName("Curated list has over 140 selective companies")
    void registry_hasExtensiveCoverage() {
        var companies = TargetCompanyRegistry.getAllCompanies();
        assertTrue(companies.size() >= 140, "Registry should have 140+ curated companies");
    }

    @Test
    @DisplayName("Matches Tier-1 and Big Tech with subsidiary names")
    void matches_tier1Tech() {
        // Standard names
        assertTrue(TargetCompanyRegistry.matches("Google", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Microsoft", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Amazon", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Apple", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Meta", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("NVIDIA", null, null, null));

        // Subsidiary and registered entities in India
        assertTrue(TargetCompanyRegistry.matches("Google India Private Limited", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Amazon Development Centre India", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("NVIDIA Graphics Pvt Ltd", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Microsoft India R&D Pvt Ltd", null, null, null));
    }

    @Test
    @DisplayName("Matches Global Banks and GCC engineering organizations")
    void matches_banksAndGccs() {
        assertTrue(TargetCompanyRegistry.matches("JPMorgan Chase & Co.", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Goldman Sachs Services LLC", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Morgan Stanley India", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Bank of America", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("American Express Technology", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Wells Fargo India Solutions", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Walmart Global Tech India", null, null, null));
    }

    @Test
    @DisplayName("Matches Indian Tech Unicorns and Fintech leaders")
    void matches_indianUnicorns() {
        assertTrue(TargetCompanyRegistry.matches("Razorpay Software Private Limited", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("CRED", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Swiggy (Bundl Technologies)", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Zepto (KiranaKart)", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Groww", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Zerodha", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Ather Energy", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Postman", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("BrowserStack", null, null, null));
    }

    @Test
    @DisplayName("Matches Quantitative Trading and HFT firms")
    void matches_quantHft() {
        assertTrue(TargetCompanyRegistry.matches("Citadel Securities", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Tower Research Capital India", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Jane Street", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Optiver", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Graviton Research Capital", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Two Sigma Investments", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("D. E. Shaw India Pvt Ltd", null, null, null));
    }

    @Test
    @DisplayName("Matches Frontier AI research labs and specialized AI startups")
    void matches_frontierAi() {
        assertTrue(TargetCompanyRegistry.matches("OpenAI", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Anthropic", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Perplexity AI", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Scale AI", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Cohere", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Mistral AI", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Sarvam AI", null, null, null));
    }

    @Test
    @DisplayName("Matches Semiconductor, Systems, and Strategy leaders")
    void matches_semiconductorAndStrategy() {
        assertTrue(TargetCompanyRegistry.matches("Qualcomm India Private Limited", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Advanced Micro Devices (AMD)", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Texas Instruments India", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Intel Corporation", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("McKinsey & Company", null, null, null));
        assertTrue(TargetCompanyRegistry.matches("Boston Consulting Group (BCG)", null, null, null));
    }

    @Test
    @DisplayName("Rejects mass IT recruiters and third-party staffing agencies")
    void rejects_massRecruitersAndStaffing() {
        assertFalse(TargetCompanyRegistry.matches("Tata Consultancy Services", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("TCS", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("Infosys Limited", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("Wipro Technologies", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("Cognizant Technology Solutions", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("HCL Technologies", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("Tech Mahindra", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("Accenture Solutions Pvt Ltd", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("Capgemini India", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("LTIMindtree", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("Randstad India Staffing", null, null, null));
        assertFalse(TargetCompanyRegistry.matches("Kelly Services", null, null, null));
    }

    @Test
    @DisplayName("Category filtering restricts matches to selected sectors")
    void categoryFilter_restrictsMatches() {
        Set<String> quantOnly = Set.of(TargetCompanyRegistry.CAT_QUANT_HFT);
        assertTrue(TargetCompanyRegistry.matches("Citadel", null, quantOnly, null));
        assertTrue(TargetCompanyRegistry.matches("Tower Research", null, quantOnly, null));
        assertFalse(TargetCompanyRegistry.matches("Flipkart", null, quantOnly, null));
        assertFalse(TargetCompanyRegistry.matches("Salesforce", null, quantOnly, null));

        Set<String> aiOnly = Set.of(TargetCompanyRegistry.CAT_FRONTIER_AI);
        assertTrue(TargetCompanyRegistry.matches("OpenAI", null, aiOnly, null));
        assertTrue(TargetCompanyRegistry.matches("Anthropic", null, aiOnly, null));
        assertFalse(TargetCompanyRegistry.matches("Barclays", null, aiOnly, null));
    }

    @Test
    @DisplayName("Custom whitelist overrides general matching")
    void customWhitelist_filtersStrictly() {
        Set<String> customList = Set.of("NVIDIA", "Razorpay");
        assertTrue(TargetCompanyRegistry.matches("NVIDIA Graphics", null, null, customList));
        assertTrue(TargetCompanyRegistry.matches("Razorpay Software", null, null, customList));
        assertFalse(TargetCompanyRegistry.matches("Google", null, null, customList));
        assertFalse(TargetCompanyRegistry.matches("Citadel", null, null, customList));
    }

    @Test
    @DisplayName("URL matching identifies companies from career portal links")
    void matches_viaUrlKeywords() {
        assertTrue(TargetCompanyRegistry.matches("Unknown Entity", "https://careers.google.com/jobs/123", null, null));
        assertTrue(TargetCompanyRegistry.matches(null, "https://amazon.jobs/en/jobs/456", null, null));
        assertTrue(TargetCompanyRegistry.matches("", "https://boards.greenhouse.io/anthropic/jobs/789", null, null));
    }

    @Test
    @DisplayName("URL matching does NOT false-positive on UTM params like google_jobs_apply")
    void rejects_utmFalsePositives() {
        // SerpAPI/Google Jobs results have google_jobs_apply in UTM params — must NOT match "Google"
        assertFalse(TargetCompanyRegistry.matches("Zynfos Solutions",
                "https://in.jobrapido.com/jobpreview/12345?utm_campaign=google_jobs_apply&utm_source=google_jobs_apply",
                null, null));
        assertFalse(TargetCompanyRegistry.matches("Random Startup",
                "https://bebee.com/in/jobs/some-job?utm_campaign=google_jobs_apply&utm_medium=organic",
                null, null));

        // Adzuna India URLs for non-target companies — must NOT match
        assertFalse(TargetCompanyRegistry.matches("AU Small Finance Bank",
                "https://www.adzuna.in/land/ad/12345?se=abc&utm_medium=api",
                null, null));

        // Indeed URLs for non-target companies — must NOT match
        assertFalse(TargetCompanyRegistry.matches("SystemDR LLP",
                "https://in.indeed.com/viewjob?jk=abc123&utm_campaign=google_jobs_apply",
                null, null));

        // But direct company URLs should still match
        assertTrue(TargetCompanyRegistry.matches("Unknown",
                "https://stripe.com/careers/listing/software-engineer-intern/8031833",
                null, null));
        assertTrue(TargetCompanyRegistry.matches("Unknown",
                "https://www.adzuna.in/details/12345?utm_medium=api",
                null, null) == false, "Adzuna generic URL should not match any target");
    }
}
