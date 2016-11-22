package inventory.mysql;

import inventory.mysql.models.Inventory;
import inventory.mysql.models.IInventoryRepo;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;


import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

/**
 * REST Controller to manage Inventory database
 *
 */
@RestController
public class InventoryController {

	@Autowired
	private IInventoryRepo itemsRepo;

	private Iterable<Inventory>	cachedItems = new ArrayList<Inventory>();
	/**
	 * check
	 */
	@RequestMapping("/check")
	@ResponseBody String check() {
		return "it works!";
	}

	/**
	 * @return all items in inventory
	 */
	@HystrixCommand(fallbackMethod="getInventoryCached")
	@RequestMapping(value = "/inventory", method = RequestMethod.GET)
	@ResponseBody Iterable<Inventory> getInventory() {
		cachedItems = itemsRepo.findAll();
		return cachedItems;
	}

	/**
	 * @return item by id
	 */
	@HystrixCommand(fallbackMethod="getCachedItemById")
	@RequestMapping(value = "/inventory/{id}", method = RequestMethod.GET)
	@ResponseBody Inventory getById(@PathVariable long id) {
			return itemsRepo.findOne(id);
	}

	/**
	 * @return item(s) containing name
	 */
	@RequestMapping(value = "/inventory/name/{name}", method = RequestMethod.GET)
	@ResponseBody List<Inventory> getByName(@PathVariable String name) {
			return itemsRepo.findByNameContaining(name);
	}

	/**
	 * @return item(s) by price lte
	 */
	@RequestMapping(value = "/inventory/price/{price}", method = RequestMethod.GET)
	@ResponseBody List<Inventory> getByPrice(@PathVariable int price) {
			return itemsRepo.findByPriceLessThanEqual(price);
	}

	/**
	 * Add Item
	 * @return transaction status
	 */
	@RequestMapping(value = "/inventory", method = RequestMethod.POST, consumes = "application/json")
	@ResponseBody String create(@RequestBody Inventory payload) {
		try {
			itemsRepo.save(payload);
		}
		catch (Exception ex) {
			return "Error adding item to inventory: " + ex.toString();
		}
		return "Item succesfully added to inventory! (id = " + payload.getId() + ")";
	}


	/**
	 * Update Item
	 * @return transaction status
	 */
	@RequestMapping(value = "/inventory/update/{id}", method = RequestMethod.PUT, consumes = "application/json")
	@ResponseBody String update(@PathVariable long id, @RequestBody Inventory payload) {
		try {
			if (itemsRepo.exists(id)) {
				payload.setId(id);
				itemsRepo.save(payload);
			} else
				return "Item not found, nothing to update.";
		}
		catch (Exception ex) {
			return "Error updating item: " + ex.toString();
		}
		return "Item succesfully updated!";
	}

	/**
	 * Delete Item
	 * @return transaction status
	 */
	@RequestMapping(value = "/inventory/delete/{id}", method = RequestMethod.DELETE)
	@ResponseBody String delete(@PathVariable long id) {
		try {
			if (itemsRepo.exists(id))
				itemsRepo.delete(id);
			else
				return "Item not found, nothing to delete.";
		}
		catch (Exception ex) {
			return "Error deleting item:" + ex.toString();
		}
		return "Item succesfully deleted from inventory!";
	}

	private Iterable<Inventory> getInventoryCached() {
		// Return cached items
		return cachedItems;
	}

	/**
	 * @return cached item by id
	 */
	private Inventory getCachedItemById(long id) {

		Iterator<Inventory> iter = cachedItems.iterator();
		while (iter.hasNext()) {
			Inventory item = iter.next();
			if (item.getId() == id){
				return item;
			}
		}

		return new Inventory();
	}
	/**
	 * @return Cirtcuit breaker tripped
	 */
	@HystrixCommand(fallbackMethod="getInventoryCached")
	@RequestMapping("/circuitbreaker")
	@ResponseBody
	public String tripCircuitBreaker() {
		System.out.println("Circuitbreaker Service invoked");
		return "";
	}
}
