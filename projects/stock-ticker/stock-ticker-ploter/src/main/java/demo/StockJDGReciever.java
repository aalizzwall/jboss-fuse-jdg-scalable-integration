package demo;

import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.Event;
import org.jboss.demo.jdg.model.StockHistoryQuote;

@Listener
public class StockJDGReciever {
	
	private StockPloter ploter;

	public StockJDGReciever(StockPloter ploter) {
		super();
		this.ploter = ploter;
	}

	@CacheEntryCreated
	@CacheEntryModified
	public void handle(Event<String,StockHistoryQuote> event) {	
		if( !event.isPre() && ( event.getType()==Event.Type.CACHE_ENTRY_MODIFIED || event.getType()==Event.Type.CACHE_ENTRY_CREATED) ) {
			StockHistoryQuote nextValue = event.getType()==Event.Type.CACHE_ENTRY_CREATED ? ((CacheEntryCreatedEvent<String,StockHistoryQuote>) event).getValue() : ((CacheEntryModifiedEvent<String,StockHistoryQuote>)event).getValue();
			ploter.add(nextValue);
			
		}
	}
}
