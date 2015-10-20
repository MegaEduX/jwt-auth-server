package models;

import com.avaje.ebean.Model;
import play.data.validation.Constraints;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Created by MegaEduX on 20/10/15.
 */

@Entity
public class Demo extends Model {
    @Id
    @GeneratedValue
    public Integer id;

    public String foo;
}
