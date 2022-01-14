/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.metrics.scopus.script;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.dspace.app.cris.metrics.common.model.ConstantMetrics;
import org.dspace.app.cris.metrics.common.model.CrisMetrics;
import org.dspace.app.cris.metrics.common.services.MetricsPersistenceService;
import org.dspace.app.cris.metrics.scopus.dto.ScopusResponse;
import org.dspace.app.cris.metrics.scopus.services.ScopusService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.DiscoverResult.SearchDocument;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SolrServiceImpl;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;

public class ScriptRetrieveCitation {

	private static final String fieldPubmedID = ConfigurationManager.getProperty("cris", "ametrics.identifier.pmid");
	private static final String fieldScopusID = ConfigurationManager.getProperty("cris", "ametrics.identifier.eid");
	private static final String fieldDoiID = ConfigurationManager.getProperty("cris", "ametrics.identifier.doi");
	private static final Integer maxItemsInOneCall = ConfigurationManager.getIntProperty("cris", "ametrics.elsevier.scopus.call.max_items", 10);

	/** log4j logger */
	private static Logger log = Logger.getLogger(ScriptRetrieveCitation.class);

	private static MetricsPersistenceService pService;

	private static ScopusService sService;

	private static SearchService searcher;

	private static long timeElapsed = 3600000 * 24 * 7; // 1 week

	private static int maxItemToWork = 100;

	private static String queryDefault;

	private static int MAX_QUERY_RESULTS = 50;

	private static boolean sleep = true;

	private static boolean enrichMetadataItem = false;

	public static void main(String[] args)
			throws SearchServiceException, SQLException, AuthorizeException, ParseException {

		CommandLineParser parser = new PosixParser();

		Options options = new Options();
		options.addOption("h", "help", false, "help");

		options.addOption("e", "enrich", false, "Enrich item with the response from scopus");

		options.addOption("s", "disable-sleep", false, "disable sleep timeout for each call to scopus");

		options.addOption("t", "time", true,
				"Limit to update only citation more old than <t> seconds. Use 0 to force update of all record");

		options.addOption("x", "max", true,
				"Process a max of <x> items. Only worked items matter, item not worked because up-to-date (see t option) are not counted. Use 0 to set no limits");

		options.addOption("q", "query", true,
				"Override the default query to retrieve puntual publication (used for test scope, the default query will be deleted");

		CommandLine line = parser.parse(options, args);

		if (line.hasOption('h')) {
			HelpFormatter myhelp = new HelpFormatter();
			myhelp.printHelp("RetrieveCitation \n", options);
			System.out.println("\n\nUSAGE:\n RetrieveCitation [-t 3600] [-x 100] \n");
			System.exit(0);
		}

		DSpace dspace = new DSpace();
		int citationRetrieved = 0;
		int itemWorked = 0;
		int itemForceWorked = 0;
		Date startDate = new Date();

		if (line.hasOption('t')) {
			timeElapsed = Long.valueOf(line.getOptionValue('t').trim()) * 1000; // option
																				// is
																				// in
																				// seconds
		}
		if (line.hasOption('x')) {
			maxItemToWork = Integer.valueOf(line.getOptionValue('x').trim());
			if (maxItemToWork < MAX_QUERY_RESULTS) {
				MAX_QUERY_RESULTS = maxItemToWork;
			}
		}

		queryDefault = fieldPubmedID + ":[* TO *] OR " + fieldDoiID + ":[* TO *] OR " + fieldScopusID + ":[* TO *]";
		if (line.hasOption('q')) {
			queryDefault = line.getOptionValue('q').trim();
		}

		if (line.hasOption('s')) {
			sleep = false;
		}

		if (line.hasOption('e')) {
			enrichMetadataItem = true;
		}

		ServiceManager serviceManager = dspace.getServiceManager();

		searcher = serviceManager.getServiceByName(SearchService.class.getName(), SearchService.class);

		pService = serviceManager.getServiceByName(MetricsPersistenceService.class.getName(),
				MetricsPersistenceService.class);

		sService = serviceManager.getServiceByName(ScopusService.class.getName(), ScopusService.class);

		Context context = null;
		long resultsTot = -1;
		try {
			context = new Context();
			context.turnOffAuthorisationSystem();
			List<ScopusIdentifiersToRequest> scopusList = new ArrayList<ScopusIdentifiersToRequest>(); 
			all: for (int page = 0;; page++) {
				int start = page * MAX_QUERY_RESULTS;
				if (resultsTot != -1 && start >= resultsTot) {
					break all;
				}
				if (maxItemToWork != 0 && itemWorked >= maxItemToWork  && itemForceWorked > 50)
					break all;

				SolrQuery solrQuery = new SolrQuery(queryDefault);
				solrQuery.setStart(start);
				solrQuery.setRows(MAX_QUERY_RESULTS);
				solrQuery.addFilterQuery(SolrServiceImpl.RESOURCE_TYPE_FIELD + ":" + Constants.ITEM);
				solrQuery.addField(fieldPubmedID);
				solrQuery.addField(fieldDoiID);
				solrQuery.addField(fieldScopusID);
		        solrQuery.addField(SolrServiceImpl.HANDLE_FIELD);
		        solrQuery.addField(SolrServiceImpl.RESOURCE_TYPE_FIELD);
		        solrQuery.addField(SolrServiceImpl.RESOURCE_ID_FIELD);
				// get all items that contains PMID or DOI or EID
				QueryResponse qresp = searcher.search(solrQuery);
				SolrDocumentList results = qresp.getResults();
				resultsTot = results.getNumFound();
				log.info(LogManager.getHeader(null, "retrieve_citation_scopus",
						"Processing " + resultsTot + " items"));
                log.info(LogManager.getHeader(null, "retrieve_citation_scopus",
                        "Processing informations itemWorked:\""+itemWorked+"\" maxItemToWork: \"" + maxItemToWork + "\" - start:\"" + start + "\" - page:\"" + page + "\""));
				// for each item check
                for (SolrDocument solrDoc : results) {

						if (maxItemToWork != 0 && itemWorked >= maxItemToWork  && itemForceWorked > 50)
							break all;

						Integer itemID = (Integer)solrDoc.getFieldValue("search.resourceid");

						if (isCheckRequired(itemID)) {
							itemWorked++;
							ScopusIdentifiersToRequest sc2R = new ScopusIdentifiersToRequest();
							sc2R.setPmids((List)solrDoc.getFieldValues(fieldPubmedID));
							sc2R.setDois((List)solrDoc.getFieldValues(fieldDoiID));
							sc2R.setEids((List)solrDoc.getFieldValues(fieldScopusID));
							sc2R.setIdentifier(itemID);
							scopusList.add(sc2R);
							
							if (scopusList.size() >= maxItemsInOneCall) {
								itemForceWorked = workItems(context, scopusList, itemForceWorked);
							}
						}
				}

                context.commit();
                context.clearCache();
			}
			if (!scopusList.isEmpty()) {
				itemForceWorked = workItems(context, scopusList, itemForceWorked);
				context.commit();
			}
			
			context.complete();
			Date endDate = new Date();
			long processTime = (endDate.getTime() - startDate.getTime()) / 1000;
			log.info(LogManager.getHeader(null, "retrieve_citation", "Processing time " + processTime
					+ " sec. - Retrieved " + citationRetrieved + " Scopus citation for " + itemWorked + " items(" + itemForceWorked + " forced items)"));
		} catch (Exception ex) {
			log.error(ex.getMessage(), ex);
		} finally {
			if (context != null && context.isValid()) {
				context.abort();
			}

		}
	}
	
	private static int workItems(Context context, List<ScopusIdentifiersToRequest> scopusList, int itemForceWorked) throws SQLException, AuthorizeException {
	    List<ScopusIdentifiersToResponse> responseObjects = sService.getCitations(context, sleep, scopusList);
		
		for (ScopusIdentifiersToResponse scopusIDs2Response : responseObjects) {
			
			boolean itWorks = false;
			if (scopusIDs2Response.getResponse() != null) {
				itWorks = buildCiting(context, scopusIDs2Response.getDso(), scopusIDs2Response.getResponse());
			}
			if(itWorks) {
			    itemForceWorked++;
			}
		}
		scopusList.clear();
		return itemForceWorked;
	}

	private static boolean buildCiting(Context context, DSpaceObject dso, ScopusResponse response) throws SQLException, AuthorizeException {
        CrisMetrics citation = response.getCitation();
        if (!response.isError())
        {
            if (citation != null)
            {
                citation.setResourceId(dso.getID());
                citation.setResourceTypeId(dso.getType());
                citation.setUuid(dso.getHandle());
                citation.setContext(context);
                pService.saveOrUpdate(CrisMetrics.class, citation);
                if (enrichMetadataItem)
                {
                    Item item = (Item) dso;
                    if (StringUtils.isNotBlank(citation.getIdentifier()))
                    {
                        Metadatum[] metadatumEid = item
                                .getMetadataByMetadataString(fieldScopusID);
                        if (metadatumEid != null && metadatumEid.length > 0)
                        {
                            item.clearMetadata(metadatumEid[0].schema,
                                    metadatumEid[0].element,
                                    metadatumEid[0].qualifier,
                                    metadatumEid[0].language);
                            item.addMetadata(metadatumEid[0].schema,
                                    metadatumEid[0].element,
                                    metadatumEid[0].qualifier,
                                    metadatumEid[0].language,
                                    citation.getIdentifier());
                        }
                    }

                    item.update();
                }
                return true;
            }
        }
        return false;
	}

	private static boolean isCheckRequired(Integer itemID) {
		if (timeElapsed != 0) {
			CrisMetrics cit = pService.getLastMetricByResourceIDAndResourceTypeAndMetricsType(itemID, Constants.ITEM, ConstantMetrics.STATS_INDICATOR_TYPE_SCOPUS);
			if (cit == null || cit.getMetricCount()==-1) {
				if(cit!=null) {
					pService.delete(CrisMetrics.class, cit.getId());
				}
				return true;
			}
			long now = new Date().getTime();

			Date lastCheck = cit.getTimeStampInfo().getCreationTime();
			long lastCheckTime = 0;

			if (lastCheck != null)
				lastCheckTime = lastCheck.getTime();

			return (now - lastCheckTime >= timeElapsed);
		} else {
			return true;
		}
	}
	
	public static class ScopusIdentifiersToResponse
	{
		private DSpaceObject dso;
		private ScopusResponse response;
		
		public DSpaceObject getDso() {
			return dso;
		}
		public void setDso(DSpaceObject dso) {
			this.dso = dso;
		}
		public ScopusResponse getResponse() {
			return response;
		}
		public void setResponse(ScopusResponse response) {
			this.response = response;
		}
	}
	
    public static class ScopusIdentifiersToRequest
    {
        private List<String> pmids;
        private List<String> dois;
        private List<String> eids;
        private Integer identifier;
        
        public List<String> getPmids() {
            return pmids;
        }
        public void setPmids(List<String> pmids) {
            this.pmids = pmids;
        }
        public List<String> getDois() {
            return dois;
        }
        public void setDois(List<String> dois) {
            this.dois = dois;
        }
        public List<String> getEids() {
            return eids;
        }
        public void setEids(List<String> eids) {
            this.eids = eids;
        }
        public Integer getIdentifier()
        {
            return identifier;
        }
        public void setIdentifier(Integer identifier)
        {
            this.identifier = identifier;
        }
    }	
}