package io.inversion.utils;

public class RegexTest {
    public static void main(String[] args){
        String regex = "[+-]?([0-9]*[.])?[0-9]+";
        System.out.println("12345".matches(regex));
        System.out.println("123.45".matches(regex));

        System.out.println("-12345".matches(regex));
        System.out.println("+12345".matches(regex));
        System.out.println("123.".matches(regex));
        System.out.println("1,2,3".matches(regex));

        regex = "([+-]?([0-9]*[.])?[0-9]+)((,[+-]?([0-9]*[.])?[0-9]+))*";
        System.out.println("1,3.".matches(regex));

        System.out.println("1".matches(regex));



    }
}
