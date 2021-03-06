
package org.itsnat.spistless;

import org.itsnat.spi.SPIMainDocumentConfig;
import org.itsnat.spi.SPIStateDescriptor;
import org.itsnat.core.ClientDocument;
import org.itsnat.core.event.ItsNatEventDOMStateless;
import org.itsnat.core.http.ItsNatHttpServletRequest;
import org.itsnat.core.http.ItsNatHttpServletResponse;
import org.itsnat.spi.SPIMainDocument;
import org.itsnat.spi.SPIState;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

public abstract class SPIStlessMainDocument extends SPIMainDocument
{
    public SPIStlessMainDocument(ItsNatHttpServletRequest request, ItsNatHttpServletResponse response,SPIMainDocumentConfig config)
    {
        super(request, response,config);

        if (itsNatDoc.isCreatedByStatelessEvent())
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
