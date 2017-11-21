import imp

conf = imp.load_source("pkg_conf", "../pkg_conf.py")

pkg = {
    "name"          : "cloud-trace",
    "version"       : conf.glassfish_version,
    "attributes"    : {
                        "pkg.summary" : "Cloud Trace Integration",
                        "pkg.description" : "Payara Cloud Trace modules",
                        "info.classification" : "OSGi Service Platform Release 4",
                      },
    "dirtrees"      : { "glassfish/modules" : {},
                      },
    "licenses"      : {
                        "../../../../ApacheLicense.txt" : {"license" : "ApacheV2"},
                      }
 }
 
