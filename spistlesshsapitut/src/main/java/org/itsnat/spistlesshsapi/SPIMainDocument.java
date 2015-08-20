
package org.itsnat.spistlesshsapi;

import org.itsnat.spi.SPIMainDocumentConfig;
import org.itsnat.spi.SPIStateDescriptor;
import javax.servlet.http.HttpServletRequest;
import org.itsnat.core.ClientDocument;
import org.itsnat.core.ItsNatServlet;
import org.itsnat.core.event.ItsNatEventDOMStateless;
import org.itsnat.core.html.ItsNatHTMLDocument;
import org.itsnat.core.http.ItsNatHttpServletRequest;
import org.itsnat.core.http.ItsNatHttpServletResponse;
import org.itsnat.core.tmpl.ItsNatDocFragmentTemplate;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

public abstract class SPIMainDocument
{
    protected ItsNatHTMLDocument itsNatDoc;
    protected SPIMainDocumentConfig config;
    protected String title;
    protected String googleAnalyticsIFrameURL;

    public SPIMainDocument(ItsNatHttpServletRequest request, ItsNatHttpServletResponse response,SPIMainDocumentConfig config)
    {
        config.check();

        this.config = config;
        this.itsNatDoc = (ItsNatHTMLDocument)request.getItsNatDocument();

        this.title = config.getTitleElement().getText(); // Initial value
        this.googleAnalyticsIFrameURL = config.getGoogleAnalyticsElement().getAttribute("src");  // Initial value

        if (!itsNatDoc.isCreatedByStatelessEvent())
        {
            String stateName;
            HttpServletRequest servReq = request.getHttpServletRequest();
            String reqURI = servReq.getRequestURI();

            if (!reqURI.endsWith("/"))
            {
                // Pretty URL case  
                int pos = reqURI.lastIndexOf("/");
                stateName = reqURI.substring(pos + 1); // "/name" => "name"             
            }
            else  
            {  
                stateName = config.getDefaultStateName();
            }

            changeState(stateName);
        }
        else
        {
            EventListener listener = new EventListener()
            {
                @Override
                public void handleEvent(Event evt)
                {
                    ItsNatEventDOMStateless itsNatEvt = (ItsNatEventDOMStateless)evt;

                    String stateName = (String)itsNatEvt.getExtraParam("state_name");

                    changeState(stateName,itsNatEvt);
                }
            };

            itsNatDoc.addEventListener(listener);
        }
    }

    public ItsNatHTMLDocument getItsNatHTMLDocument()
    {
        return itsNatDoc;
    }

    public SPIStateDescriptor getSPIStateDescriptor(String stateName)
    {
        return config.getSPIStateDescriptor(stateName);
    }

    public void setStateTitle(String stateTitle)
    {
        String pageTitle = stateTitle + " - " + title;
        if (itsNatDoc.isLoading())
            config.getTitleElement().setText(pageTitle);
        else
            itsNatDoc.addCodeToSend("document.title = \"" + pageTitle + "\";\n");
    }

    public Element getContentParentElement()
    {
        return config.getContentParentElement();
    }

    public ItsNatDocFragmentTemplate getFragmentTemplate(String name)
    {
        ItsNatServlet servlet = itsNatDoc.getItsNatDocumentTemplate().getItsNatServlet();
        return servlet.getItsNatDocFragmentTemplate(name);
    }

    public DocumentFragment loadDocumentFragment(String name)
    {
        ItsNatDocFragmentTemplate template = getFragmentTemplate(name);
        if (template == null)
            throw new RuntimeException("There is no template registered for state or fragment name: " + name);
        return template.loadDocumentFragment(itsNatDoc);
    }

    public String getFirstLevelStateName(String stateName)
    {
        String firstLevelName = stateName;
        int pos = stateName.indexOf('-');
        if (pos != -1) firstLevelName = stateName.substring(0, pos); // Case "overview-popup"
        return firstLevelName;
    }

    public SPIState changeState(String stateName)
    {    
        return changeState(stateName,null);
    }
    
    public SPIState changeState(String stateName,ItsNatEventDOMStateless itsNatEvt)
    {
        SPIStateDescriptor stateDesc = config.getSPIStateDescriptor(stateName);
        if (stateDesc == null)
        {
            return changeState(config.getNotFoundStateName(),itsNatEvt);
        }        
        
        // Cleaning previous state:
        if (!itsNatDoc.isLoading())
        {
            ClientDocument clientDoc = itsNatDoc.getClientDocumentOwner();
            String contentParentRef = clientDoc.getScriptUtil().getNodeReference(config.getContentParentElement());            
            clientDoc.addCodeToSend("window.spiSite.removeChildren(" + contentParentRef + ");");  // ".innerHTML = '';"
        }

        // Setting new state:
        changeActiveMenu(stateName);

        String fragmentName = stateDesc.isMainLevel() ? stateName : getFirstLevelStateName(stateName);        
        DocumentFragment frag = loadDocumentFragment(fragmentName);
        config.getContentParentElement().appendChild(frag);

        return createSPIState(stateDesc,itsNatEvt);
    }
    
    public abstract SPIState createSPIState(SPIStateDescriptor stateDesc,ItsNatEventDOMStateless itsNatEvt);

    public void registerState(SPIState state)
    {
        setStateTitle(state.getStateTitle());
        String stateName = state.getStateName();
        itsNatDoc.addCodeToSend("spiSite.setStateInURL(\"" + stateName + "\");");
        // googleAnalyticsElem.setAttribute("src",googleAnalyticsIFrameURL + stateName);
        // http://stackoverflow.com/questions/24407573/how-can-i-make-an-iframe-not-save-to-history-in-chrome
        String jsIFrameRef = itsNatDoc.getScriptUtil().getNodeReference(config.getGoogleAnalyticsElement());
        itsNatDoc.addCodeToSend("var elem = " + jsIFrameRef + "; try{ elem.contentWindow.location.replace('" + googleAnalyticsIFrameURL + stateName + "'); } catch(e) {}");
    }


    public void changeActiveMenu(String stateName)
    {
        String mainMenuItemName = getFirstLevelStateName(stateName);

        Element currentMenuItemElem = config.getMenuElement(mainMenuItemName);        
        for(Element menuItemElem : config.getMenuElementMap().values())
        {
            onChangeActiveMenu(menuItemElem,(currentMenuItemElem == menuItemElem));
        }
    }
    
    public abstract void onChangeActiveMenu(Element menuItemElem,boolean active);
}
