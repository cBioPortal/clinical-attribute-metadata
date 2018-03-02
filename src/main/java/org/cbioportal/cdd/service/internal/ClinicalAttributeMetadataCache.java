/*
 * Copyright (c) 2018 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

package org.cbioportal.cdd.service.internal;

import javax.annotation.PostConstruct;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.cbioportal.cdd.model.ClinicalAttributeMetadata;
import org.cbioportal.cdd.repository.ClinicalAttributeMetadataRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author Robert Sheridan, Manda Wilson
 */
@Component
@EnableScheduling
public class ClinicalAttributeMetadataCache {

    private static HashMap<String, ClinicalAttributeMetadata> clinicalAttributeCache = new HashMap<String, ClinicalAttributeMetadata>();
    private static HashMap<String, Map<String, ClinicalAttributeMetadata>> overridesCache = new HashMap<String, Map<String, ClinicalAttributeMetadata>>();

    private static final Logger logger = LoggerFactory.getLogger(ClinicalAttributeMetadataCache.class);

    @Autowired
    private ClinicalAttributeMetadataRepository clinicalAttributesRepository;

    public Map<String, ClinicalAttributeMetadata> getClinicalAttributeMetadata() {
        return Collections.unmodifiableMap(clinicalAttributeCache);
    }
    
    public Map<String, Map<String, ClinicalAttributeMetadata>> getClinicalAttributeMetadataOverrides() {
        return Collections.unmodifiableMap(overridesCache);
    }
    
    @PostConstruct // call when constructed
    @Scheduled(cron="0 */5 * * * *") // call every 5 minutes
    private void resetCache() {
        // TODO make sure we don't have two scheduled calls run simultaneously
        logger.info("resetCache(): refilling clinical attribute cache");
        List<ClinicalAttributeMetadata> latestClinicalAttributeMetadata = clinicalAttributesRepository.getClinicalAttributeMetadata();
        HashMap<String, ClinicalAttributeMetadata> latestClinicalAttributeMetadataCache = new HashMap<String, ClinicalAttributeMetadata>();
        if (latestClinicalAttributeMetadata.size() > 0) {
            for (ClinicalAttributeMetadata clinicalAttributeMetadata : latestClinicalAttributeMetadata) {
                latestClinicalAttributeMetadataCache.put(clinicalAttributeMetadata.getNormalizedColumnHeader(), clinicalAttributeMetadata);
            }
            clinicalAttributeCache = latestClinicalAttributeMetadataCache;
            logger.info("resetCache(): refillled cache with " + latestClinicalAttributeMetadata.size() + " clinical attributes");
        } else {
            // what if cache never gets updated because we break something?
            logger.error("resetCache(): failed to pull clinical attributes from repository, not updating cache");
        }
   
        // latestOverrides is a map of study-id to list of overridden ClinicalAttributeMetadata objects
        // latestOverridesCache is a map of study-id to map of clinical attribute name to overridden ClinicalAttributeMetadata object 
        Map<String, ArrayList<ClinicalAttributeMetadata>> latestOverrides = clinicalAttributesRepository.getClinicalAttributeMetadataOverrides();
        HashMap<String, Map<String,ClinicalAttributeMetadata>> latestOverridesCache = new HashMap<String, Map<String, ClinicalAttributeMetadata>>();
        if (latestOverrides.size() > 0) {
            for (Map.Entry<String, ArrayList<ClinicalAttributeMetadata>> entry : latestOverrides.entrySet()) {
		HashMap<String, ClinicalAttributeMetadata> clinicalAttributesMetadataMapping = new HashMap<String, ClinicalAttributeMetadata>();
		for (ClinicalAttributeMetadata clinicalAttributeMetadata : entry.getValue()) {
			clinicalAttributesMetadataMapping.put(clinicalAttributeMetadata.getNormalizedColumnHeader(), clinicalAttributeMetadata);
		}
                latestOverridesCache.put(entry.getKey(), clinicalAttributesMetadataMapping);
            }
            overridesCache = latestOverridesCache;
            logger.info("resetCache(): refilled overrides cache with " + latestOverrides.size() + " overrides");
        } else {
            logger.error("resetCache(): failed to pull overrides from repository, not updating cache");
        }
    }
} 

