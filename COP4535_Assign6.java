import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

class LCP
{
    
    public static void main(String[] args) {
        for (String str : args) {
            try {
                if (args.length == 0) {
                    System.out.println("ERROR invalid inputs");
                    break;
                }
                System.out.print("\n" + str + " ");
                double start = System.nanoTime();
                construct(str);
                double stop = System.nanoTime();
                double math = (stop - start) / 1000000000;
                System.out.printf("TIME: %.4f seconds\n", math);
            } catch (IOException e) {
                System.err.println("Error processing" + str);
            } catch (NumberFormatException n) {//checks for wrong input being passed
                System.err.println("Error processing" + str);
            }
        }
       // test("AAABAABAABBB");
        //test("aaaaaa");
    }
    
    static void construct(String f) throws FileNotFoundException{
        String tester = "";
        ArrayList<String> file = new ArrayList<>();
        Scanner in = new Scanner(new FileReader(f));
        String temp = "";
        while(in.hasNextLine()){
            temp = in.nextLine();
         //   file.add(temp);
            tester = tester + temp;
        }
        test(tester);
        
    }
    
    /*
     * Create the LCP array from the suffix array
     * @param s the input array populated from 0..N-1, with available pos N
     * @param sa the already-computed suffix array 0..N-1
     * @param LCP the resulting LCP array 0..N-1
     */
    public static void makeLCPArray( int [ ] s, int [ ] sa, int [ ] LCP )
    {
        int N = sa.length;
        int [ ] rank = new int[ N ];
        
        s[ N ] = -1;
        for( int i = 0; i < N; i++ )
            rank[ sa[ i ] ] = i;
        
        int h = 0;
        for( int i = 0; i < N; i++ )
            if( rank[ i ] > 0 )
            {
                int j = sa[ rank[ i ] - 1 ];
                
                while( s[ i + h ] == s[ j + h ] )
                    h++;
                
                LCP[ rank[ i ] ] = h;
                if( h > 0 )
                    h--;
            }
    }
    
    /*
     * Fill in the suffix array information for String str
     * @param str the input String
     * @param sa existing array to place the suffix array
     */
    public static void createSuffixArray( String str, int [ ] sa, int [ ] LCP )
    {        
        int N = str.length( );
        
        int [ ] s = new int[ N + 3 ];
        int [ ] SA = new int[ N + 3 ];
        
        for( int i = 0; i < N; i++ )
            s[ i ] = str.charAt( i );
        
        makeSuffixArray( s, SA, N, 256 );
        
        for( int i = 0; i < N; i++ )
            sa[ i ] = SA[ i ];
        
        makeLCPArray( s, sa, LCP );
    }
    
    
    // find the suffix array SA of s[0..n-1] in {1..K}^n
    // require s[n]=s[n+1]=s[n+2]=0, n>=2
    public static void makeSuffixArray( int [ ] s, int [ ] SA, int n, int K )
    {
        int n0 = ( n + 2 ) / 3;
        int n1 = ( n + 1 ) / 3;
        int n2 = n / 3;
        int t = n0 - n1;  // 1 iff n%3 == 1
        int n12 = n1 + n2 + t;

        int [ ] s12  = new int[ n12 + 3 ];
        int [ ] SA12 = new int[ n12 + 3 ];
        int [ ] s0   = new int[ n0 ];
        int [ ] SA0  = new int[ n0 ];
        
        // generate positions in s for items in s12
        // the "+t" adds a dummy mod 1 suffix if n%3 == 1
        // at that point, the size of s12 is n12
        for( int i = 0, j = 0; i < n + t; i++ )
            if( i % 3 != 0 )
                s12[ j++ ] = i;
        
        int K12 = assignNames( s, s12, SA12, n0, n12, K );
  
        computeS12( s12, SA12, n12, K12 );
        computeS0( s, s0, SA0, SA12, n0, n12, K );
        merge( s, s12, SA, SA0, SA12, n, n0, n12, t );
    }
    
    // Assigns the new supercharacter names.
    // At end of routine, SA will have indices into s, in sorted order
    // and s12 will have new character names
    // Returns the number of names assigned; note that if
    // this value is the same as n12, then SA is a suffix array for s12.
    private static int assignNames( int [ ] s, int [ ] s12, int [ ] SA12,
                                   int n0, int n12, int K )
    {
           // radix sort the new character trios
        radixPass( s12 , SA12, s, 2, n12, K );
        radixPass( SA12, s12 , s, 1, n12, K );  
        radixPass( s12 , SA12, s, 0, n12, K );

          // find lexicographic names of triples
        int name = 0;
        int c0 = -1, c1 = -1, c2 = -1;
      
        for( int i = 0; i < n12; i++ )
        {
            if( s[ SA12[ i ] ] != c0 || s[ SA12[ i ] + 1 ] != c1
                                     || s[ SA12[ i ] + 2 ] != c2 )
            { 
                name++;
                c0 = s[ SA12[ i ] ];
                c1 = s[ SA12[ i ] + 1 ];
                c2 = s[ SA12[ i ] + 2 ];
            }
          
            if( SA12[ i ] % 3 == 1 )
                s12[ SA12[ i ] / 3 ]      = name;
            else
                s12[ SA12[ i ] / 3 + n0 ] = name; 
       }
      
       return name;
    }
    
    
    // stably sort in[0..n-1] with indices into s that has keys in 0..K
    // into out[0..n-1]; sort is relative to offset into s
    // uses counting radix sort
    private static void radixPass( int [ ] in, int [ ] out, int [ ] s, int offset,
                                  int n, int K ) 
    { 
        int [ ] count = new int[ K + 2 ];            // counter array
        
        for( int i = 0; i < n; i++ )
            count[ s[ in[ i ] + offset ] + 1 ]++;    // count occurences
        
        for( int i = 1; i <= K + 1; i++ )            // compute exclusive sums
            count[ i ] += count[ i - 1 ];

        for( int i = 0; i < n; i++ )
            out[ count[ s[ in[ i ] + offset ] ]++ ] = in[ i ];      // sort
    } 
    
    // stably sort in[0..n-1] with indices into s that has keys in 0..K
    // into out[0..n-1]
    // uses counting radix sort
    private static void radixPass( int [ ] in, int [ ] out, int [ ] s, int n, int K ) 
    { 
        radixPass( in, out, s, 0, n, K );
    }
   

    // Compute the suffix array for s12, placing result into SA12
    private static void computeS12( int [ ] s12, int [ ] SA12, int n12, int K12 )
    {
        if( K12 == n12 ) // if unique names, don't need recursion
            for( int i = 0; i < n12; i++ )
                SA12[ s12[i] - 1 ] = i; 
        else
        {
            makeSuffixArray( s12, SA12, n12, K12 );
            // store unique names in s12 using the suffix array 
            for( int i = 0; i < n12; i++ )
                s12[ SA12[ i ] ] = i + 1;
        }
    }
    
    private static void computeS0( int [ ] s, int [ ] s0, int [ ] SA0, int [ ] SA12,
                               int n0, int n12, int K )
    {
        for( int i = 0, j = 0; i < n12; i++ )
            if( SA12[ i ] < n0 )
                s0[ j++ ] = 3 * SA12[ i ];
        
        radixPass( s0, SA0, s, n0, K );
    }
    
    
    // merge sorted SA0 suffixes and sorted SA12 suffixes
    private static void merge( int [ ] s, int [ ] s12,
                              int [ ] SA, int [ ] SA0, int [ ] SA12,
                              int n, int n0, int n12, int t )
    {      
        int p = 0, k = 0;
        
        while( t != n12 && p != n0 )
        {
            int i = getIndexIntoS( SA12, t, n0 ); // S12
            int j = SA0[ p ];                     // S0
            
            if( suffix12IsSmaller( s, s12, SA12, n0, i, j, t ) )
            { 
                SA[ k++ ] = i;
                t++;
            }
            else
            { 
                SA[ k++ ] = j;
                p++;
            }  
        } 
        
        while( p < n0 )
            SA[ k++ ] = SA0[ p++ ];
        while( t < n12 )
            SA[ k++ ] = getIndexIntoS( SA12, t++, n0 ); 
    }
    
    private static int getIndexIntoS( int [ ] SA12, int t, int n0 )
    {
        if( SA12[ t ] < n0 )
            return SA12[ t ] * 3 + 1;
        else
            return ( SA12[ t ] - n0 ) * 3 + 2;
    }
    
    private static boolean leq( int a1, int a2, int b1, int b2 )
      { return a1 < b1 || a1 == b1 && a2 <= b2; }
    
    private static boolean leq( int a1, int a2, int a3, int b1, int b2, int b3 )
      { return a1 < b1 || a1 == b1 && leq( a2, a3,b2, b3 ); }
    
    private static boolean suffix12IsSmaller( int [ ] s, int [ ] s12, int [ ] SA12,
                                             int n0, int i, int j, int t )
    {
        if( SA12[ t ] < n0 )  // s1 vs s0; can break tie after 1 character
            return leq( s[ i ], s12[ SA12[ t ] + n0 ],
                        s[ j ], s12[ j / 3 ] );
        else                  // s2 vs s0; can break tie after 2 characters
            return leq( s[ i ], s[ i + 1 ], s12[ SA12[ t ] - n0 + 1 ],
                        s[ j ], s[ j + 1 ], s12[ j / 3 + n0 ] );
    }

    public static void printV( int [ ]  a, String comment )
    {
        System.out.print( comment + ":" );
        for( int x : a ) 
            System.out.print( x + " " );

        System.out.println( );
    }

    public static boolean isPermutation( int [ ] SA, int n )
    {
        boolean [ ] seen = new boolean [ n ];
        
        for( int i = 0; i < n; i++ )
            seen[ i ] = false;
        
        for( int i = 0; i < n; i++ )
            seen[ SA[ i ] ] = true;
        
        for( int i = 0; i < n; i++ )
            if( !seen[ i ] )
                return false;
        
        return true;
    }

    public static boolean sleq( int  [ ] s1, int start1, int [ ] s2, int start2 )
    {
        for( int i = start1, j = start2; ; i++, j++ )
        {
            if( s1[ i ] < s2[ j ] )
                return true;
            
            if( s1[ i ] > s2[ j ] )
                return false;
        }
    } 

    // Check if SA is a sorted suffix array for s
    public static boolean isSorted( int [ ] SA, int [ ] s, int n )
    {
        for( int i = 0; i < n-1; i++ )
            if( !sleq( s, SA[ i ], s, SA[ i + 1 ] ) )
                return false;
      
        return true;  
    }



    public static void assert0( boolean cond )
    {
        if( !cond )
            throw new AssertionException( );
    }


    public static void test( String str )
    {
        int [ ] sufarr = new int[ str.length( ) ];
        int [ ] LCP = new int[ str.length( ) ];

        createSuffixArray( str, sufarr, LCP );
        
        Suffix [] Suffixarr = new Suffix[sufarr.length];
        for(int i = 0; i < str.length(); i++){
            Suffixarr[i]=new Suffix(sufarr[i], LCP[i]);
           // System.out.println(str.substring(sufarr[i], sufarr.length));
        }
        
        HashMap<Integer, LinkedList<Suffix>> longest_seq = new HashMap<>();
        for(int i = 2; i<=10;i++){
            longest_seq.put(i, new LinkedList<Suffix>());
        }
        
        find_longest(Suffixarr,longest_seq, str);
        for(Integer i : longest_seq.keySet()){
            if(!longest_seq.get(i).isEmpty()){
                if(longest_seq.get(i).getFirst().suffix_length<=50){
                    System.out.println("Longest sequence that occurs " +i+ " times has basic length "+ longest_seq.get(i).getFirst().suffix_length + " and is "+ str.substring(longest_seq.get(i).getFirst().suffix_start, longest_seq.get(i).getFirst().suffix_start + longest_seq.get(i).getFirst().suffix_length));
                } else {
                 //   System.out.println("Longest sequence that occurs " +i+ " times has basic length "+ longest_seq.get(i).getFirst().suffix_length + " and is "+ str.substring(longest_seq.get(i).getFirst().suffix_start, longest_seq.get(i).getFirst().suffix_start+20)+"..."+str.substring(longest_seq.get(i).getFirst().suffix_start+longest_seq.get(i).getFirst().suffix_length, longest_seq.get(i).getFirst().suffix_length));
                }
            } else {
                System.out.println("Longest sequence that occurs " +i+ " times didn't occur");
            }
           /* System.out.print(i+" ");
            
            for(Suffix s : longest_seq.get(i)){
                System.out.print(str.substring(s.suffix_start, str.length())+ ", ");
            }
            System.out.println();
        }
        //System.out.println(longest_seq.entrySet());
        
       /* System.out.println( str + ":" );
        for( int i = 0; i < str.length( ); i++ )
            System.out.println( i + " " + sufarr[ i ] + " " + LCP[ i ] );
        System.out.println( );*/
    }
    }
    
    static void find_longest(Suffix[] arr, HashMap<Integer, LinkedList<Suffix>> longest_seq, String str) {

        //deal with first case. don't start at 0, start at 1
        int k = 0;
        LinkedList<Suffix> seq = new LinkedList<>();
        for (int i = 1; i < arr.length; i++) {
            int min = Integer.MAX_VALUE / 3;
            for (Integer key : longest_seq.keySet()) {
                if (k < min) {
                    min = k;
                }
            }
            if (min < arr[i].suffix_length) {
                checkten(i, arr, seq, k, str);
                k = seq.size();
            }
            if (k >= 2 && k <= 10) {
                if (longest_seq.get(k).isEmpty() || seq.get(0).suffix_length > longest_seq.get(k).get(0).suffix_length) {
                    longest_seq.get(k).clear();
                    for (Suffix s : seq) {
                        longest_seq.get(k).add(s);
                    }
                }
                k = 0;
                seq.clear();
            } else {
                k = 0;
                seq.clear();
            }
          //   System.out.println(arr[i].suffix);
            
            
            
            
         /*   if(arr[i-1].suffix_length == arr[i].suffix_length){
                k++;
                seq.add(arr[i-1]);
            }
            else if(arr[i-1].suffix_length != arr[i].suffix_length && k>=2 && k<=10){
                seq.add(arr[i-1]);
                if(longest_seq.get(k).isEmpty() || seq.get(0).suffix_length>longest_seq.get(k).get(0).suffix_length){
                    longest_seq.get(k).clear();
                    for(Suffix s: seq){
                        longest_seq.get(k).add(s);                   
                    }
                    k=0;
                    seq.clear();                    
                }
            }
            else{
                k=0;
                seq.clear();
            }*/
        }
        //as long as the length matches that's how long the k should be
        
    }
    
    public static void checkten(int i, Suffix[] arr, LinkedList<Suffix> seq, int k, String str) {
        int LCP_length = arr[i].suffix_length;
        for (int j = i; j > (i - 10) && 0 <= j; j--) {
            if (str.substring(arr[j].suffix_start, str.length()).length() >= LCP_length) {
                String temp1 = str.substring(arr[i].suffix_start, arr[i].suffix_start+arr[i].suffix_length);
                String temp2 = str.substring(arr[j].suffix_start, arr[j].suffix_start+arr[i].suffix_length);
                if (temp1.equals(temp2)&& !temp1.equals("")) {
                    seq.add(arr[j]);
                    k = k++;
                } else {
                    break;
                }
            } else {
                break;
            }

        }
    }
    
    static int minimum(int [] key){
        int min = Integer.MAX_VALUE/3;
        for(Integer k: key){
            if(k<min)
                min = k;
        }
        return min;       
    }

    /*
     * Returns the LCP for any two strings
     */
    public static int computeLCP( String s1, String s2 )
    {
        int i = 0;
        
        while( i < s1.length( ) && i < s2.length( ) && s1.charAt( i ) == s2.charAt( i ) )
            i++;
        
        return i;
    }

    /*
     * Fill in the suffix array and LCP information for String str
     * @param str the input String
     * @param SA existing array to place the suffix array
     * @param LCP existing array to place the LCP information
     * Note: Starting in Java 7, this will use quadratic space.
     */
    public static void createSuffixArraySlow( String str, int [ ] SA, int [ ] LCP )
    {
        if( SA.length != str.length( ) || LCP.length != str.length( ) )
            throw new IllegalArgumentException( );
        
        int N = str.length( );
        
        String [ ] suffixes = new String[ N ];
        for( int i = 0; i < N; i++ )
            suffixes[ i ] = str.substring( i );
        
        Arrays.sort( suffixes );
        
        for( int i = 0; i < N; i++ )
            SA[ i ] = N - suffixes[ i ].length( );
        
        LCP[ 0 ] = 0;
        for( int i = 1; i < N; i++ )
            LCP[ i ] = computeLCP( suffixes[ i - 1 ], suffixes[ i ] );
    }
}

class AssertionException extends RuntimeException
{
}

class Suffix
{
    String suffix;
    int suffix_start;
    int suffix_length;
    
    Suffix(int start, int length){
        this.suffix_start = start;
        suffix_length = length;
    }
}
