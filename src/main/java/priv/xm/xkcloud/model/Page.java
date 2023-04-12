package priv.xm.xkcloud.model;

public class Page {
    private int currentpage;
    private int pagesize;
    private int startindex;
    private String searchcontent;
    private int user_id;
    
    public Page() {
        pagesize = 5;
        currentpage = 1;
    }

    public int getCurrentpage() {
        return currentpage;
    }

    public void setCurrentpage(int currentpage) {
        this.currentpage = (currentpage < 1) ? 1 : currentpage;
    }

    public int getPagesize() {
        return pagesize;
    }

    public void setPagesize(int pageSize) {
        this.pagesize = (pageSize < 1) ? 5 :pageSize;
    }

    public int getStartindex() {
        this.startindex = (this.currentpage-1)*this.pagesize;
        return startindex;
    }

    public void setStartindex(int startindex) {
        this.startindex = startindex;
    }

    public String getSearchcontent() {
        return searchcontent;
    }

    public void setSearchcontent(String searchcontent) {
        this.searchcontent = searchcontent;
    }
    
    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }
}
