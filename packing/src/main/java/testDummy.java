import com.uc.wms.annotation.Level;
import com.uc.wms.annotation.Lock;
import com.uc.wms.annotation.Locks;
import com.uc.wms.aspect.locking.Namespace;

public class testDummy {

    @Locks({ @Lock(ns = Namespace.PICKLIST, key = "#{#args[0].picklistCode}", level = Level.FACILITY) })
    public void getNumber(){

    }
}
