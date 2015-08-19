package demo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.infinispan.Cache;
import org.infinispan.commons.util.CloseableIteratorCollection;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.demo.jdg.model.StockHistoryQuote;
import org.jboss.demo.jdg.model.StockQuote;

public class StockPloter extends JPanel {

	private static final float LINE_THICKNESS = 1.2f;

	private static final long serialVersionUID = -6701507664970285335L;

	private static final double RELATIV_SCREEN_AREA = 0.95;
	public static final int WIDTH = 520;
	public static final int HEIGHT = 400;
	private static final int NUM_ELEMENTS = 500;
	public static final int EXTRA_WIDTH_FOR_ORDERS = 250;
	
	private double maxStockValue = 70;
	public  int pixelsPerPoint = Math.round(WIDTH-20/NUM_ELEMENTS);
	private String orderlist = "";

	
	
	public static final String cacheName = "stock-ticker-cache";
	public static final String cacheOrderName = "stock-order-cache";
	public static final String[] stockSymbols = {"RHT"};
	
	private ArrayDeque<StockHistoryQuote> stockHistoryData = new ArrayDeque<StockHistoryQuote>(NUM_ELEMENTS);
	private ArrayDeque<StockQuote> stockOrderData = new ArrayDeque<StockQuote>(NUM_ELEMENTS);
	
	Cache<String,StockHistoryQuote> cache;

	public StockPloter() throws IOException {
		super();
		EmbeddedCacheManager cacheManager;
		cacheManager = new DefaultCacheManager("infinispan.xml");
		Cache<String,StockHistoryQuote> cache = cacheManager.getCache(cacheName);
		cache.addListener(new StockJDGReciever(this));
		cache.start();
		
		Cache<String,StockQuote> orderCache = cacheManager.getCache(cacheOrderName);
		orderCache.addListener(new OrderJDGReciever(this));
		orderCache.start();
		
		CloseableIteratorCollection<StockHistoryQuote> values = cache.values();
		System.out.format("The cache container has %d entries\n",values.size());
		
		CloseableIteratorCollection<StockQuote> orderShares = orderCache.values();
		System.out.format("The order cache container has %d entries\n",orderShares.size());
		
		List<StockHistoryQuote> list = new ArrayList<StockHistoryQuote>(values);
		List<StockQuote> orderlist = new ArrayList<StockQuote>(orderShares);
		
		/**Collections.sort( list , new Comparator< StockHistoryQuote >( ){
			@Override
			public int compare(StockHistoryQuote o1, StockHistoryQuote o2) {
				return o1.getId()-o2.getId();
			}
		} ); **/
		
		for(StockHistoryQuote quote : (list.size()<NUM_ELEMENTS) ? list : (list.subList(list.size()-NUM_ELEMENTS, list.size()))) {
			if(quote.getAdjClose()>maxStockValue)
				maxStockValue=quote.getAdjClose();
			this.stockHistoryData.add(quote);
		}
		
		for(StockQuote orderQuote : (orderlist.size()<NUM_ELEMENTS) ? orderlist : (orderlist.subList(orderlist.size()-NUM_ELEMENTS, orderlist.size()))) {
			this.stockOrderData.add(orderQuote);
		}
		this.repaint();
		
	}


	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Color.RED);
		g.setFont(new Font(Font.SANS_SERIF,Font.BOLD,10));
		
		pixelsPerPoint = getWidth()/NUM_ELEMENTS;
		
		double oldValue=-1.0;
		
		if (stockHistoryData!=null && stockHistoryData.size()>0) {
			int x=0;
			for (StockHistoryQuote quote : stockHistoryData) {
				if (oldValue > 0) {
					double newValue = quote.getAdjClose();
					g2.setStroke(new BasicStroke(LINE_THICKNESS));
					g2.drawLine(x, scaleAndPositionValue(oldValue), x+=pixelsPerPoint, scaleAndPositionValue(newValue));
				}
				oldValue = quote.getAdjClose();
			}
			 
			double currentValue = stockHistoryData.getLast().getAdjClose();
			String currentValueStr = String.format("CurrentValue: %g \n", currentValue);	
			g.drawChars(currentValueStr.toCharArray(), 0, currentValueStr.length(), getWidth()/2-50, getHeight()-10);
		}
		if (stockOrderData!=null && stockOrderData.size()>0) {
			//Print out ordered shares, if any
			int orderheight = 10;
			double earnvalue = 0.0;
			for (StockQuote order : stockOrderData) {
				earnvalue =(order.getVolume() * stockHistoryData.getFirst().getAdjClose()) - (order.getVolume()* order.getCloseValue());
				orderlist = String.format("[%s] bought %d shares at value", order.getCustId() ,order.getVolume());
				String earnlist = String.format(" $%s, and earns $%s today ", roundDouble(order.getCloseValue()), roundDouble(earnvalue));
				if(orderlist != null && !"".equals(orderlist.trim())){
					g.drawChars(orderlist.toCharArray(), 0, orderlist.length(), getWidth()-EXTRA_WIDTH_FOR_ORDERS + 10, 50+orderheight);
					orderheight += 10;
					g.drawChars(earnlist.toCharArray(), 0, earnlist.length(), getWidth()-EXTRA_WIDTH_FOR_ORDERS + 10, 50+orderheight);
					orderheight += 15;
				}
			}
		}
	}
	
	
	private int scaleAndPositionValue(double value) {
		double heightInDrawArea = this.getHeight()*RELATIV_SCREEN_AREA;
		double pixelsPerValuePoint = heightInDrawArea/maxStockValue;
		int middleOfPanel=(int) Math.round(this.getHeight()/2);
		int scaledValue = (int) Math.round(value*pixelsPerValuePoint);
		int positionedScaledValue = middleOfPanel - (scaledValue - (int) Math.round(heightInDrawArea/2));
//		System.out.format("Scaled value %d to x-cordinate %d\n",value,positionedScaledValue);
		return positionedScaledValue;
	}
	
	public void add(StockHistoryQuote quote) {
		this.stockHistoryData.add(quote);
		if(stockHistoryData.size()>NUM_ELEMENTS) {
			stockHistoryData.removeFirst(); //We will only save enough data to print on the screen
		}
		if(quote.getAdjClose()>maxStockValue)
			maxStockValue=quote.getAdjClose();
		this.repaint();
	}

	public void addOrder(StockQuote order) {
		this.stockOrderData.add(order);
		if(stockOrderData.size()>NUM_ELEMENTS) {
			stockOrderData.removeFirst(); //We will only save enough data to print on the screen
		}
		
		this.repaint();
	}
	
	private String roundDouble(double val){
		DecimalFormat df2 = new DecimalFormat("#########.##");
		
        return df2.format(val);
	}
	
	public static void main(String[] args) throws IOException {
		//String symbol = JOptionPane.showInputDialog("Enter Ticker Symbol:");
		String symbol = stockSymbols[0];
		StockPloter panel = new StockPloter();	
		JFrame frame = new JFrame(String.format("Data for stock %s", symbol));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel.setBackground(Color.WHITE);
		frame.setBackground(Color.WHITE);
		frame.add(panel);
		frame.setSize(WIDTH+EXTRA_WIDTH_FOR_ORDERS, HEIGHT); 
		frame.setVisible(true);
		
	}
	
}
