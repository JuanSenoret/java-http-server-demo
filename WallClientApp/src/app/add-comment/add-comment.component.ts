import { Component, OnInit } from '@angular/core';
import { BackendService } from '../services/backend.service';
import { AddCommentData } from '../models/add-comment-data.model'
import { SnackbarComponent } from '../snackbar/snackbar.component';
import {MatSnackBar, MatSnackBarConfig} from '@angular/material';

@Component({
  selector: 'app-add-comment',
  templateUrl: './add-comment.component.html',
  styleUrls: ['./add-comment.component.scss']
})
export class AddCommentComponent implements OnInit {

  private nameValue: string = '';
  private commentValue: string = '';
  private addComment: AddCommentData;
  private configSnackbarSuccess: MatSnackBarConfig = {
    panelClass: 'snackbar-style-success',
    duration: 10000,
    verticalPosition: 'bottom'
  };
  private configSnackbarError: MatSnackBarConfig = {
    panelClass: 'snackbar-style-error',
    duration: 10000,
    verticalPosition: 'bottom'
  };

  constructor(
    private backendService: BackendService,
    private snackBar: MatSnackBar
  ) { }

  ngOnInit() {
  }

  submit() {
    this.addComment = {
      username: this.nameValue,
      comment: this.commentValue
    };

    this.backendService.addComment(this.addComment).subscribe((response) =>{
      console.log(response['status']);
      if (response['status'] && response['status'] === 'DONE') {
        this.snackBar.openFromComponent(SnackbarComponent, {
          data: `${this.nameValue}'s comment successfully added`,
          ...this.configSnackbarSuccess
        });
      } else {
        this.snackBar.openFromComponent(SnackbarComponent, {
          data: `Error: Comment NOT added`,
          ...this.configSnackbarError
        });
      }
    });
  }

}
