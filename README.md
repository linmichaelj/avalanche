avalanche
=========

Flexible and fast batch processing for real-time data generating applications

Basic Test
==========






Note
====
As mentioned in the thesis, the data generated is persisted on machines between each jobs.  To clear the database between runs the "clearLocalDB" script found in the root directory should be executed on any machine running an instance of the DataHandlerService or DataManagerService.  Services should be stopped before this lock is run to prevent contention.   
