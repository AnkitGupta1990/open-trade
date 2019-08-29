
import java.text.DecimalFormat;
import java.util.List;

public class OptionData implements Cloneable {
	
	DecimalFormat df = new DecimalFormat("#.##");
	
	private String symbol;
	private String type;
	private Double strikePrice = 0d;
	private Double reverseStrikePrice = 0d;
	
	private Double gain = 0d;
	private Double buy = 0d;
	private Double sell = 0d;

	private Double currentPrice = 0d;
	
	private Double previousClose = 0d;
	private Double open = 0d;
	private Double low = 0d;
	
	private Double buyPrice = 0d;
	private Double sellPrice = 0d;
	private Double targetSellPrice = 0d;
	
	private boolean isSell = false;
	private boolean reverseTrade = false;
	private boolean executeReverseTrade = false;
		
	public OptionData(String symbol, String type) {
		super();
		this.symbol = symbol;
		this.type = type;
	}
	
	public Double getCurrentPrice() {
		return currentPrice;
	}
	public void setCurrentPrice(Double currentPrice) {
		this.currentPrice = currentPrice;
	}
	public String getSymbol() {
		return symbol;
	}
	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public Double getStrikePrice() {
		return strikePrice;
	}
	public void setStrikePrice(Double strikePrice) {
		this.strikePrice = Double.parseDouble(df.format(strikePrice));
	}
	public Double getReverseStrikePrice() {
		return reverseStrikePrice;
	}
	public void setReverseStrikePrice(Double reverseStrikePrice) {
		this.reverseStrikePrice = reverseStrikePrice;
	}
	public Double getGain() {
		return gain;
	}
	public void setGain(Double gain) {
		this.gain = Double.parseDouble(df.format(gain));
	}
	public Double getBuy() {
		return buy;
	}
	public void setBuy(Double buy) {
		this.buy = Double.parseDouble(df.format(buy));
	}
	public Double getSell() {
		return sell;
	}
	public void setSell(Double sell) {
		this.sell = Double.parseDouble(df.format(sell));
	}
	public Double getPreviousClose() {
		return previousClose;
	}
	public void setPreviousClose(Double previousClose) {
		this.previousClose = Double.parseDouble(df.format(previousClose));
	}
	public Double getOpen() {
		return open;
	}
	public void setOpen(Double open) {
		this.open = Double.parseDouble(df.format(open));
	}
	public Double getLow() {
		return low;
	}
	public void setLow(Double low) {
		this.low = Double.parseDouble(df.format(low));
	}
	public Double getBuyPrice() {
		return buyPrice;
	}
	public void setBuyPrice(Double buyPrice) {
		this.buyPrice = buyPrice;
	}
	public Double getSellPrice() {
		return sellPrice;
	}
	public void setSellPrice(Double sellPrice) {
		this.sellPrice = sellPrice;
	}
	public boolean isSell() {
		return isSell;
	}
	public void setSell(boolean isSell) {
		this.isSell = isSell;
	}
	public boolean isReverseTrade() {
		return reverseTrade;
	}
	public void setReverseTrade(boolean reverseTrade) {
		this.reverseTrade = reverseTrade;
	}
	public boolean isExecuteReverseTrade() {
		return executeReverseTrade;
	}
	public void setExecuteReverseTrade(boolean executeReverseTrade) {
		this.executeReverseTrade = executeReverseTrade;
	}
	public Double getTargetSellPrice() {
		return targetSellPrice;
	}
	public void setTargetSellPrice(Double targetSellPrice) {
		this.targetSellPrice = targetSellPrice;
	}
	
	public static OptionData getReverseTrade(OptionData data, List<OptionData> list) {
		for(OptionData data2 : list) {
			if(data2.isReverseTrade()) {
				if(data2.getSymbol().equalsIgnoreCase(data.getSymbol())) {
					return data2;
				}
			}
		}
		return null;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public String toString() {
		return String.format("%1$15s", symbol) + ", " + type + ", " + String.format("%1$5s",df.format(strikePrice)) + 
				", Gain = " + String.format("%1$10s",df.format(gain)) + "	" + df.format(buy) + "	" + df.format(sell);
	}
}
