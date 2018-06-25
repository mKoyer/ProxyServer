public class DomainStats
{
    private String domain;
    private long requestAmount;
    private long inputData;
    private long outputData;

    public DomainStats(String domain, long requestAmount, long inputData, long outputData)
    {
        this.domain = domain;
        this.requestAmount = requestAmount;
        this.inputData = inputData;
        this.outputData = outputData;
    }

    public String getDomain()
    {
        return domain;
    }

    public void setDomain(String domain)
    {
        this.domain = domain;
    }

    public long getRequestAmount()
    {
        return requestAmount;
    }

    public void setRequestAmount(long requestAmount)
    {
        this.requestAmount = requestAmount;
    }

    public long getInputData()
    {
        return inputData;
    }

    public void setInputData(long inputData)
    {
        this.inputData = inputData;
    }

    public long getOutputData()
    {
        return outputData;
    }

    public void setOutputData(long outputData)
    {
        this.outputData = outputData;
    }

}
