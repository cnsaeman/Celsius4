import java.io.*;

public class test
{
	public static void main(String[] args) {
		String test="@Article{Klauder:2018:1-15,\n   journal = \"Springer Proceedings in Physics\",\n   volume = \"\",\n   journal = \"Springer Proceedings in Physics\",\n }";
		System.out.println(test.replaceAll("\\s+volume\\s+\\= \\\"\\\",",""));
	}
}
