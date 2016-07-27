package peerLink;
import java.io.Serializable;
class Pair<T1, T2> implements Serializable{
	private static final long serialVersionUID = 2L;
	private T1 x;
	private T2 y;

	public Pair(){}

	public Pair(T1 a, T2 b){
		x = a;
		y = b;	
	}

	public T1 getX(){
		return this.x;
	}

	public T2 getY(){
		return this.y;
	}

	public void setX(T1 a){
		this.x = a;
	}

	public void setY(T2 a){
		this.y = a;
	}

}
