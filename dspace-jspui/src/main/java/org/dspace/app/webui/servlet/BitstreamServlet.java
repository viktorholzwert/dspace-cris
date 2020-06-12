/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.IViewer;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.eperson.EPerson;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.plugin.BitstreamHomeProcessor;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageEvent;

/**
 * Servlet for retrieving bitstreams. The bits are simply piped to the user. If
 * there is an <code>If-Modified-Since</code> header, only a 304 status code
 * is returned if the containing item has not been modified since that date.
 * <P>
 * <code>/bitstream/handle/sequence_id/filename</code>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class BitstreamServlet extends DSpaceServlet
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(BitstreamServlet.class);

    /**
     * Threshold on Bitstream size before content-disposition will be set.
     */
    private int threshold;
    
    // services API
    private final transient HandleService handleService
             = HandleServiceFactory.getInstance().getHandleService();
    
    private final transient BitstreamService bitstreamService
             = ContentServiceFactory.getInstance().getBitstreamService();
    
    @Override
	public void init(ServletConfig arg0) throws ServletException {
		super.init(arg0);
		threshold = ConfigurationManager
				.getIntProperty("webui.content_disposition_threshold");
	}

    @Override
	protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	Item item = null;
    	Bitstream bitstream = null;

        boolean displayLicense = ConfigurationManager.getBooleanProperty("webui.licence_bundle.show", false);
        boolean isLicense = false;
        boolean displayPreservation = ConfigurationManager.getBooleanProperty("webui.preservation_bundle.show", false);
        boolean isPreservation = false;    	
        // Get the ID from the URL
        String idString = request.getPathInfo();
        String handle = "";
        String sequenceText = "";
        String filename = null;
        int sequenceID;

        if (idString == null)
        {
            idString = "";
        }
        
        // Parse 'handle' and 'sequence' (bitstream seq. number) out
        // of remaining URL path, which is typically of the format:
        // {handle}/{sequence}/{bitstream-name}
        // But since the bitstream name MAY have any number of "/"s in
        // it, and the handle is guaranteed to have one slash, we
        // scan from the start to pick out handle and sequence:

        // Remove leading slash if any:
        if (idString.startsWith("/"))
        {
            idString = idString.substring(1);
        }

        // skip first slash within handle
        int slashIndex = idString.indexOf('/');
        if (slashIndex != -1)
        {
            slashIndex = idString.indexOf('/', slashIndex + 1);
            if (slashIndex != -1)
            {
                handle = idString.substring(0, slashIndex);
                int slash2 = idString.indexOf('/', slashIndex + 1);
                if (slash2 != -1)
                {
                    sequenceText = idString.substring(slashIndex+1,slash2);
                    filename = idString.substring(slash2+1);
                }
            }
        }

        try
        {
            sequenceID = Integer.parseInt(sequenceText);
        }
        catch (NumberFormatException nfe)
        {
            sequenceID = -1;
        }
        
        // Now try and retrieve the item
        DSpaceObject dso = handleService.resolveToObject(context, handle);
        
        // Make sure we have valid item and sequence number
        if (dso != null && dso.getType() == Constants.ITEM && sequenceID >= 0)
        {
            item = (Item) dso;
        
            if (item.isWithdrawn())
            {
                log.info(LogManager.getHeader(context, "view_bitstream",
                        "handle=" + handle + ",withdrawn=true"));
                JSPManager.showJSP(request, response, "/tombstone.jsp");
                return;
            }

            boolean found = false;

            List<Bundle> bundles = item.getBundles();

            for (int i = 0; (i < bundles.size()) && !found; i++)
            {
                List<Bitstream> bitstreams = bundles.get(i).getBitstreams();

                for (int k = 0; (k < bitstreams.size()) && !found; k++)
                {
                    if (sequenceID == bitstreams.get(k).getSequenceID())
                    {
                        bitstream = bitstreams.get(k);
                        found = true;
                    }
                }
                if (found && 
                        bundles.get(i).getName().equals(Constants.LICENSE_BUNDLE_NAME) &&
                        bitstream.getName().equals(Constants.LICENSE_BITSTREAM_NAME) )
                    {
                            isLicense = true;
                }else if(found && bundles.get(i).getName().equals("PRESERVATION")) {
                    	isPreservation = true;
                }
                    
                if (!AuthorizeServiceFactory.getInstance().getAuthorizeService().isAdmin(context, bitstream) && 
                		( (isLicense && !displayLicense ) || (isPreservation && !displayPreservation ) ))
                {
                    throw new AuthorizeException();
                }
            }
        }

        if (bitstream == null || filename == null
                || !filename.equals(bitstream.getName()))
        {
            // No bitstream found or filename was wrong -- ID invalid
            log.info(LogManager.getHeader(context, "invalid_id", "path="
                    + idString));
            JSPManager.showInvalidIDError(request, response, idString,
                    Constants.BITSTREAM);

            return;
        }

        log.info(LogManager.getHeader(context, "view_bitstream",
                "bitstream_id=" + bitstream.getID()));
 
		if (bitstream.getMetadataValue(IViewer.METADATA_STRING_PROVIDER).contains(IViewer.STOP_DOWNLOAD)
				&& !AuthorizeServiceFactory.getInstance().getAuthorizeService().isAdmin(context, bitstream)) {
			throw new AuthorizeException("Download not allowed by viewer policy");
		}

        // Modification date
        // Only use last-modified if this is an anonymous access
        // - caching content that may be generated under authorisation
        //   is a security problem
		EPerson ep = context.getCurrentUser();
        if ( ep == null)
        {
            // TODO: Currently the date of the item, since we don't have dates
            // for files
            response.setDateHeader("Last-Modified", item.getLastModified()
                    .getTime());

            // Check for if-modified-since header
            long modSince = request.getDateHeader("If-Modified-Since");

            if (modSince != -1 && item.getLastModified().getTime() < modSince)
            {
                // Item has not been modified since requested date,
                // hence bitstream has not; return 304
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                Context contextFireEvent = new Context();
                DSpaceServicesFactory.getInstance().getEventService().fireEvent(
                		new UsageEvent(
                				UsageEvent.Action.VIEW, 
                				request, 
                				contextFireEvent, 
                				bitstream));

                contextFireEvent.complete();

                return;
            }
        }
        
        preProcessBitstreamHome(context, request, response, bitstream);
        
        // Pipe the bits
        InputStream is = bitstreamService.retrieve(context, bitstream);
     
		// Set the response MIME type
        response.setContentType(bitstream.getFormat(context).getMIMEType());

        // Response length
        response.setHeader("Content-Length", String
                .valueOf(bitstream.getSize()));

		if(threshold != -1 && bitstream.getSize() >= threshold)
		{
			UIUtil.setBitstreamDisposition(bitstream.getName(), request, response);
		}

        //DO NOT REMOVE IT - WE NEED TO FREE DB CONNECTION TO AVOID CONNECTION POOL EXHAUSTION FOR BIG FILES AND SLOW DOWNLOADS
        context.complete();
        
        Utils.bufferedCopy(is, response.getOutputStream());
        is.close();
        response.getOutputStream().flush();

        Context contextFireEvent = new Context();
        if(ep!= null) {
        	contextFireEvent.setCurrentUser(ep);
        }
        
        DSpaceServicesFactory.getInstance().getEventService().fireEvent(
        		new UsageEvent(
        				UsageEvent.Action.VIEW, 
        				request, 
        				contextFireEvent, 
        				bitstream));

        contextFireEvent.complete();
        
    }
    
    private void preProcessBitstreamHome(Context context, HttpServletRequest request,
            HttpServletResponse response, Bitstream item)
        throws ServletException, IOException, SQLException
    {
        try
        {
            BitstreamHomeProcessor[] chp = (BitstreamHomeProcessor[]) CoreServiceFactory.getInstance().getPluginService().getPluginSequence(BitstreamHomeProcessor.class);
            for (int i = 0; i < chp.length; i++)
            {
                chp[i].process(context, request, response, item);
            }
        }
        catch (Exception e)
        {
            log.error("caught exception: ", e);
            throw new ServletException(e);
        }
    }
}