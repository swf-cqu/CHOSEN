## SITAR Replication Results 

```
SITAR
|____project_name
|____OpenData_kfold.ipynb: the source code can replicate the published results of SITAR.
```


## RQ1: Empirical Study
```
Empirical Study
|___380 samples: the dataset used in empirical study.
|___Best: the constrcuted 3,3567 production-test change pairs.
|___Results.et: statistical results for 150 projects, analytical results for 380 samples.
```

## RQ2: The effectiveness of our method
```
RQ2
|___CHOSEN_Partial: the 343 samples are identificied by our method.
|___CHOSEN_Random: the 380 samples are identificied by our method.
|___Original_data: the sampled 380 samples to evaluate the effectiveness of identifcation method.
|___Random: the 380 samples are identificied by RG.
|___SITAR_Partial: the 343 samples are identificied by SITAR_IM.
```


## RQ3: The impact of noise on the detection method's performance
```

RQ3
|___project_name
|___|___Train: original training set.
|___|___TestSample: the sampled testing set.
|___TimeLine_newdata_SITAR.ipynb: The source code for RQ3.
```
TestSample folder:
* label: the label identified by SITAR_IM.
* true_label: manual label.


## RQ4.1 The usefulness of our method on testing set

### Data set description
```
RQ4.1
|___project_name
|___|___Train: original training set.
|___|___TestSample_Rule: the corrected testing set.
|___TimeLine_newdata_SITAR.ipynb: The source code for RQ4.1.
```
TestSample_Rule folder:
* label: the label identified by SITAR_IM.
* true_label: the label identified by our method.


## RQ4.2 The usefulness of our method on training set
```
RQ4.2
|___project_name
|___|___Train: the training set identified by our method.
|___|___TestSample: the clearned testing set.
```

## RQ4.3 The usefulness of our method on other projects
```
RQ4.3
|___SITAR: the training set identified by SITAR_IM.
|___SITAR_CHOSEN: the training set identified by CHOSEN.
```