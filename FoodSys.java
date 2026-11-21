import java.util.*;

class NoSuchRestaurantException extends Exception{
}
class NoSuchFoodItemException extends Exception{
}

class FoodItem{
    String name;
    double price;
    String cuisine;

    public FoodItem(String name, double price, String cuisine){
        this.name=name;
        this.price=price;
        this.cuisine=cuisine;
    }
}

class CartItem extends FoodItem{
    int tax;        //tax percentage, e.g. 5 means 5%
    int quanitity;
    public CartItem(String name, double price, String cuisine, int tax,int q){
        super(name,price,cuisine);
        this.tax=tax;
        this.quanitity=q;
    }
    public double totalPrice(){
        return this.price*this.quanitity*(1+(tax/100.0));
    }
}

class Cart{
    String restaurantName;
    List<CartItem> items=new ArrayList<>();

    public Cart(String r){
        restaurantName=r;
    }

    public boolean isEmpty(){
        return items.isEmpty();
    }

    public void addItem(CartItem c){
        //same item added again just increases the quantity
        for(CartItem i:items){
            if(i.name.equalsIgnoreCase(c.name)){
                i.quanitity+=c.quanitity;
                return;
            }
        }
        items.add(c);
    }

    //index is 1-based, as shown to the user
    public boolean deleteItem(int index){
        if(index<1||index>items.size()) return false;
        items.remove(index-1);
        return true;
    }

    public double getTotal(){
        double total=0;
        for(CartItem c:items){
            total+=c.totalPrice();
        }
        return total;
    }

    public String toString(){
        StringBuilder x=new StringBuilder();
        for(int i=0;i<items.size();i++){
            CartItem c=items.get(i);
            x.append(String.format("%d) %s x%d = %.2f RS%n",i+1,c.name,c.quanitity,c.totalPrice()));
        }
        return x.toString();
    }
}

class Restaurant{
    String name;
    int x,y;  //assume coordinates based on a grid.
    List<FoodItem> menu;
    List<Order> orders=new ArrayList<>();

    static List<Restaurant> restaurants=new ArrayList<>();
    Map<FoodItem,Integer> sales=new LinkedHashMap<>();

    public Restaurant(String name,int x,int y,List<FoodItem> menu){
        this.name=name;
        this.x=x;
        this.y=y;
        this.menu=menu;
        this.startSales();
    }
    public void showMenu(){
        int i=1;
        for(FoodItem f:menu){
            System.out.println(i+") "+f.name+" ("+f.cuisine+") - "+f.price+" RS");
            i++;
        }
    }
    //use each menu item as a key in sales, with a value of 0
    public void startSales(){
        for(FoodItem f:menu){
            sales.putIfAbsent(f,0);
        }
    }
    public void updateSales(Order o){
        for(CartItem c:o.c.items){
            //cart items are separate objects from menu items, so match on name
            for(FoodItem f:menu){
                if(f.name.equalsIgnoreCase(c.name)){
                    sales.merge(f,c.quanitity,Integer::sum);
                    break;
                }
            }
        }
    }
    public void viewSales(){
        System.out.println("Sales summary for "+name);
        double revenue=0;
        int units=0;
        for(Map.Entry<FoodItem,Integer> e:sales.entrySet()){
            double r=e.getKey().price*e.getValue();
            System.out.printf("  %-25s sold: %-3d revenue: %.2f RS%n",e.getKey().name,e.getValue(),r);
            revenue+=r;
            units+=e.getValue();
        }
        System.out.printf("  Total units: %d, total revenue (before tax): %.2f RS%n",units,revenue);
    }
    public void addOrder(Order o){
        orders.add(o);
    }
    public FoodItem getFood(String name) throws NoSuchFoodItemException{
        for(FoodItem f:menu){
            if(f.name.equalsIgnoreCase(name)) return f;
        }
        throw new NoSuchFoodItemException();
    }

    public static Restaurant getRestaurant(String name) throws NoSuchRestaurantException{
        for(Restaurant r:restaurants){
            if(r.name.equalsIgnoreCase(name)) return r;
        }
        throw new NoSuchRestaurantException();
    }
    public static void viewRestaurants(){
        int i=1;
        for(Restaurant r: restaurants){
            System.out.println(i+") "+r.name);
            i++;
        }
    }
}

class Order{
    static int nextId=1;
    static List<Order> history=new ArrayList<>();

    int id;
    String customerName;
    String restaurant;
    Cart c;
    public enum orderstatus{
        getting_prepared,
        out_for_delivery,
        delivered
    }

    long placedAt=System.currentTimeMillis();
    static final long PREPARE_MS=10*60*1000;   //first 10 min: being prepared
    static final long DELIVERY_MS=30*60*1000;  //until 30 min: out for delivery

    public Order(String cn,String r, Cart c){
        id=nextId++;
        customerName=cn;
        restaurant=r;
        this.c=c;
    }

    public orderstatus getStatus(){
        long elapsed=System.currentTimeMillis()-placedAt;
        if(elapsed<PREPARE_MS) return orderstatus.getting_prepared;
        if(elapsed<DELIVERY_MS) return orderstatus.out_for_delivery;
        return orderstatus.delivered;
    }

    public String getOrderStatus(){
        return getStatus().name();
    }

    public static Order findById(int id){
        for(Order o:history){
            if(o.id==id) return o;
        }
        return null;
    }

    public String toString(){
        return "Order #"+id+" | "+restaurant+" | "+customerName+" | "+getOrderStatus()
            +String.format(" | %.2f RS%n",c.getTotal())+c;
    }

    //places the cart as an order; the cart is emptied by the caller
    public static Order placeOrder(String customer,Cart cart) throws NoSuchRestaurantException{
        Restaurant r=Restaurant.getRestaurant(cart.restaurantName);
        Order o=new Order(customer,r.name,cart);
        history.add(o);
        r.addOrder(o);
        r.updateSales(o);
        return o;
    }
}

public class FoodSys {
    static final int TAX=5; //flat 5% tax on every item

    public static void createRestaurants(){
        Restaurant.restaurants=new ArrayList<>();

        Restaurant.restaurants.add(new Restaurant("Truffles",2,3,new ArrayList<>(List.of(
            new FoodItem("Alfredo Pasta",350,"Italian"),
            new FoodItem("Frozen Hazelnut",210,"Beverage"),
            new FoodItem("Purple Blast Bubble Tea",250,"Beverage")))));

        Restaurant.restaurants.add(new Restaurant("Wildfork",5,1,new ArrayList<>(List.of(
            new FoodItem("Crystal Dumplings",360,"Asian"),
            new FoodItem("Watermelon Salad",230,"Salad"),
            new FoodItem("Corn Ribs",190,"Snacks")))));

        Restaurant.restaurants.add(new Restaurant("Empire Restaurant",8,6,new ArrayList<>(List.of(
            new FoodItem("Chicken Shawarma",180,"Arabian"),
            new FoodItem("Hyderabadi Biryani",200,"Indian"),
            new FoodItem("Paneer Shawarma",150,"Arabian")))));

        Restaurant.restaurants.add(new Restaurant("MTR",1,7,new ArrayList<>(List.of(
            new FoodItem("Masala Dosa",150,"South Indian"),
            new FoodItem("Bisibelebath",120,"South Indian"),
            new FoodItem("Kharabath",110,"South Indian")))));

        Restaurant.restaurants.add(new Restaurant("California Burrito",6,4,new ArrayList<>(List.of(
            new FoodItem("Peri Peri Rice Bowl",145,"Mexican"),
            new FoodItem("Churos",100,"Dessert"),
            new FoodItem("Loaded Nachos",120,"Mexican")))));
    }

    public static void printChoices(){
        System.out.println("=============================================");
        System.out.println("        Food Management System               ");
        System.out.println("=============================================");
        System.out.println("1. View restaurants");
        System.out.println("2. View food menu");
        System.out.println("3. Add item to cart");
        System.out.println("4. Remove item from cart");
        System.out.println("5. Calculate total bill");
        System.out.println("6. Place order");
        System.out.println("7. Track order status");
        System.out.println("8. View order history");
        System.out.println("9. Restaurant-wise sales summary");
        System.out.println("0. Exit");
        System.out.print("Choice: ");
    }

    static Scanner sc=new Scanner(System.in);

    static String ask(String prompt){
        System.out.print(prompt);
        return sc.nextLine().trim();
    }

    static int askInt(String prompt){
        while(true){
            try{
                return Integer.parseInt(ask(prompt));
            }
            catch(NumberFormatException e){
                System.out.println("Please enter a number.");
            }
        }
    }

    static Restaurant askRestaurant(){
        try{
            return Restaurant.getRestaurant(ask("Enter restaurant name: "));
        }
        catch(NoSuchRestaurantException e){
            System.out.println("No such restaurant.");
            return null;
        }
    }

    public static void main(String[] args) {
        createRestaurants();
        Cart cart=null; //current cart, belongs to a single restaurant

        while(true){
            printChoices();
            int choice;
            try{
                choice=Integer.parseInt(sc.nextLine().trim());
            }
            catch(NumberFormatException e){
                System.out.println("Invalid choice.");
                continue;
            }
            System.out.println();

            switch(choice){
                case 0:
                    sc.close();
                    return;

                case 1:
                    Restaurant.viewRestaurants();
                    break;

                case 2: {
                    Restaurant r=askRestaurant();
                    if(r!=null) r.showMenu();
                    break;
                }

                case 3: {
                    Restaurant r=askRestaurant();
                    if(r==null) break;
                    if(cart!=null && !cart.isEmpty() && !cart.restaurantName.equalsIgnoreCase(r.name)){
                        System.out.println("Your cart has items from "+cart.restaurantName
                            +". Place the order or remove those items first.");
                        break;
                    }
                    r.showMenu();
                    String item=ask("Enter item name: ");
                    int q=askInt("Enter quantity: ");
                    if(q<=0){
                        System.out.println("Quantity must be positive.");
                        break;
                    }
                    try{
                        FoodItem f=r.getFood(item);
                        if(cart==null || cart.isEmpty()) cart=new Cart(r.name);
                        cart.addItem(new CartItem(f.name,f.price,f.cuisine,TAX,q));
                        System.out.println("Added "+q+" x "+f.name);
                    }
                    catch(NoSuchFoodItemException e){
                        System.out.println("No such food item.");
                    }
                    break;
                }

                case 4:
                    if(cart==null || cart.isEmpty()){
                        System.out.println("Your cart is empty.");
                        break;
                    }
                    System.out.print(cart);
                    if(cart.deleteItem(askInt("Enter item number to remove: ")))
                        System.out.println("Removed.");
                    else
                        System.out.println("Invalid item number.");
                    break;

                case 5:
                    if(cart==null || cart.isEmpty()){
                        System.out.println("Your cart is empty.");
                        break;
                    }
                    System.out.print(cart);
                    System.out.printf("Total bill (incl. %d%% tax): %.2f RS%n",TAX,cart.getTotal());
                    break;

                case 6:
                    if(cart==null || cart.isEmpty()){
                        System.out.println("Your cart is empty.");
                        break;
                    }
                    String cn=ask("Enter your name: ");
                    try{
                        Order o=Order.placeOrder(cn,cart);
                        System.out.println("Order placed! Your order id is "+o.id);
                        System.out.printf("Total: %.2f RS%n",o.c.getTotal());
                        cart=null;
                    }
                    catch(NoSuchRestaurantException e){
                        System.out.println("Restaurant no longer exists.");
                    }
                    break;

                case 7: {
                    Order o=Order.findById(askInt("Enter order id: "));
                    if(o==null) System.out.println("No such order.");
                    else System.out.println("Order #"+o.id+" status: "+o.getOrderStatus());
                    break;
                }

                case 8: {
                    String name=ask("Enter your name: ");
                    boolean any=false;
                    for(Order o:Order.history){
                        if(o.customerName.equalsIgnoreCase(name)){
                            System.out.println(o);
                            any=true;
                        }
                    }
                    if(!any) System.out.println("No orders found for "+name+".");
                    break;
                }

                case 9:
                    for(Restaurant r:Restaurant.restaurants){
                        r.viewSales();
                        System.out.println();
                    }
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
            System.out.println();
        }
    }
}
