# a script example to build Java project 

project =buzzbee
"build_directory" = ${~cwd~}/build
source_directory ="src/java"
doc_directory=doc
build_file ="${project}.jar"
 domain ="com"
resources ="${domain}.${project}.resources"
manifestf =""
main_class= "${domain}.${project}.Main"

websocket jar=${~cwd~}/.temp_repo/javax.websocket-api-1.1.jar
aldan3 jdo=/home/dmitriy/projects/aldan3-jdo/build/aldan3-jdo.jar
aldan3=/home/dmitriy/projects/aldan3/build/aldan3.jar
libs=[websocket jar  ,  aldan3 jdo,aldan3]

target clean {
    dependency {true}
    exec rm  (
        -r,
        ${build_directory}/${domain},
        ${build_directory}/${build_file}
    )
}

target websocket {
   dependency {
     eq {
        timestamp(websocket jar)
     }
   }
   {
      websocket_api="javax.websocket:javax.websocket-api:1.1":rep-maven
      as_url(websocket_api)
      exec wget (
        ~~, 
        -O,
        websocket jar
      )
   }
}

target compile:. {
   dependency{ target(websocket) }
   dependency {
       or {
              newerthan(${source_directory}/.java,${build_directory}/.class)
       }
   }
   {
       display(Compiling Java src ...)
       newerthan(${source_directory}/.java,${build_directory}/.class)
       assign(main src,~~)
       #display(sources ${main src} build ${build_directory})
       array(libs,${build_directory})
       scalar(~~,~path_separator~)
       exec javac (
         -d,
         ${build_directory},
        -cp,
         ${~~},
         main src
       )     
      if {
         neq(${~~}, 0)
         then {
            panic("Compilation error(s)")
         }
     }
   }
}

target jar {
      dependency {
         anynewer(${build_directory}/${domain}/*,${build_directory}/${build_file})
      }
      dependency {
          target(compile)
      }
     
     {    display(Jarring ${build_file} ...)
          exec jar (
            -cf,
            ${build_directory}/${build_file},
            -C,
            ${build_directory},
            ${domain}
          )
     }
}
