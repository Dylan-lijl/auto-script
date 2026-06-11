package pub.carzy.auto_script.entity;


/**
 * @author admin
 */
public abstract class BasicFileImport {
    private Integer order = Integer.MAX_VALUE;
    private Long id;

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
