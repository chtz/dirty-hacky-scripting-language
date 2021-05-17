# sample1.sk
Script:
```
{ 
    puts('hello world' /* <- prints "hello world" to the console */ );
    puts('hallo Welt')
}
```
Output:
```
hello world
hallo Welt
```
# sample5.sk
Script:
```
{
    l := ladd(ladd(list(),'hallo'),'welt');
    for i:=0; i<lsize(l); i:=i+1 {
        puts(lget(l,i))
    }
}
```
Output:
```
hallo
welt
```
# sample4.sk
Script:
```
{
    def oddEven(x) {
        half := x / 2;
        if x = half * 2 then {
            puts('even')
        } else {
            puts('odd')
        }
    } ;

    oddEven(3);
    oddEven(7);
    oddEven(10)
}
```
Output:
```
odd
odd
even
```
# sample7.sk
Script:
```
{
    count := 0;
    for line := gets(); not(line = null); line := gets() {
        puts('$ ' + line);
        count := count + 1;
    };
    puts(count)
}
```
Input:
```
hallo
welt
```
Output:
```
$ hallo
$ welt
2
```
# sample3.sk
Script:
```
{
    def swap(m) {
        a := mget(m,'a');
        b := mget(m,'b');
        mput(m,'a',b);
        mput(m,'b',a);
        result := m
    };

    def print(m) {
        puts(mget(m,'a') + ' ' + mget(m,'b'));
    };

    m := map();
    mput(m,'a','hello');
    mput(m,'b','world');

    print(m);
    swap(m);
    print(m)
}
```
Output:
```
hello world
world hello
```
# sample2.sk
Script:
```
{
    for i:=0; i<3; i:=i+1 { 
        puts('hallo')
    }
}
```
Output:
```
hallo
hallo
hallo
```
# sample6.sk
Script:
```
{
    puts('your name?');
    name := gets();
    puts('hello ' + name)
}
```
Input:
```
Chris
```
Output:
```
your name?
hello Chris
```
# sample9.sk
Script:
```
{
    a := 2;
    b := 4;
    c := 6;
    d := 8;
    
    puts('puts((a * b + c) * d);');
    puts((a * b + c) * d);
    
    puts('puts(a * b + c * d);');
    puts(a * b + c * d);

    puts('puts(a * (b + c) * d);');
    puts(a * (b + c) * d);
    
    if c > d then {
        puts('c > d')
    } else {
        puts('NOT c > d')
    };

    if c < d then {
        puts('c < d')
    } 
    else {
        puts('NOT c < d')
    }
}
```
Output:
```
puts((a * b + c) * d);
112
puts(a * b + c * d);
56
puts(a * (b + c) * d);
160
NOT c > d
c < d
```
# sample8.sk
Script:
```
{
    def iff(c,a,b) {
        if c then {
            if not(a=null) then {
                a();
            }
        } 
        else {
            if not(b=null) then {
                b();
            }
        }
    };

    def yes {
        puts('yes');
    };

    def no {
        puts('no');
    };

    iff(1=1,yes,no);
    iff(1=0,yes,no);
    iff(1=1,null,no);
    iff(1=0,yes,null);
}
```
Output:
```
yes
no
```
