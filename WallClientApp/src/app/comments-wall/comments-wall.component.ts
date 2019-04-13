import { Component, OnInit } from '@angular/core';
import { Comment } from '../models/comment.model';
import { BackendService } from '../services/backend.service';

@Component({
  selector: 'app-comments-wall',
  templateUrl: './comments-wall.component.html',
  styleUrls: ['./comments-wall.component.scss']
})
export class CommentsWallComponent implements OnInit {

  private comments: Comment[];

  constructor(
    private backendService: BackendService,
  ) { }

  ngOnInit() {
    this.backendService.getComments().subscribe((response: any) => {
      this.comments = response.data;
      console.log(this.comments);
    });
  }

}
